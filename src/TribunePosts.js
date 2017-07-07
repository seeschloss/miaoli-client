// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { FlatList, ScrollView, RefreshControl } from 'react-native';


import { styles } from './style';

export class TribunePosts extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      posts: [],
      refreshing: false,
      forceRefreshing: false,
    }
  }

  setRefreshing = (refreshing) => {
    this.setState({refreshing: refreshing})

    if (this.flatList) {
      this.flatList.refreshing = refreshing;
    }

    if (!refreshing) {
      this.scrollToBottom();
    }
  }

  // when provided data array changes,
  // update the internal reversed copy
  componentWillReceiveProps({ data }) {
    if (data !== this._previousData) {
      this._previousData = data;
      this.setState({ data: [...data].reverse() });
    }
  }

  scrollToBottom() {
    this.scrollToIndex({ index: 0 });
  }

  scrollToIndex(...args) {
    if (this._listViewRef) {
      this._listViewRef.scrollToIndex(...args);
    }
  }

  onScroll = (e) => {
    if (e.nativeEvent.contentOffset.y < 100) {
      this.props.tribune.showNewPostButton(true)
    } else {
      this.props.tribune.showNewPostButton(false)
    }
  }

  renderScrollComponent = ({ style, refreshing, ...props }) => (
    <ScrollView
      keyboardShouldPersistTaps={'always'}
      onScroll={this.onScroll}
      style={[style, styles.flip]}
      refreshControl={
        <RefreshControl
          refreshing={this.state.forceRefreshing}
          onRefresh={this.forceRefresh}
          />
      }
      />
  );

  appendClock = (clock) => {
    this.props.tribune.append(clock + " ")
  }

  renderItem = props => {
    return (<PostMessage tribune={this.props.tribune} post={props.item.post} />)
  };

  onEndReached = () => {
    console.log('end reached')
  }

  forceRefresh = () => {
    this.setState({refreshing: true, forceRefreshing: true})
    this.props.tribune.refreshTribune()
      .then(() => {
        this.setState({refreshing: false, forceRefreshing: false})
      })
  }

  setPosts = (posts) => {
    this.setState({
      posts: posts.map(p => { return { key: p.id, post: p } }).reverse(),
      refreshing: false,
    })
  }

  render() {
    return (
      <FlatList
        contentContainerStyle={styles.tribunePosts}
        onEndReached={this.onEndReached}
        onEndReachedThreshold={0.5}
        ref={(ref) => { this.flatList = ref }}
        extraData={this.props.tribune.lastid}
        data={this.state.posts}
        renderItem={this.renderItem}
        renderScrollComponent={this.renderScrollComponent}
        onScroll={this.onScroll}
        />
    );
  }
}

import { TouchableHighlight, Text, View, Alert, Linking, Clipboard, Share } from 'react-native';
class PostMessage extends React.Component {
  constructor(props) {
    super(props);

    this._hasOnlyEmojis = this.hasOnlyEmojis()
  }

  appendClock = () => {
    this.props.tribune.append(this.props.post.clock() + " ");
  }

  hasOnlyEmojis = () => {
    const emojiOrClockRegex = /^((((([0-9]{4})-((0[1-9])|(1[0-2]))-((0[1-9])|([12][0-9])|(3[01])))[T #])?((([01]?[0-9])|(2[0-3])):([0-5][0-9])(:([0-5][0-9]))?([:\^][0-9]|[¹²³⁴⁵⁶⁷⁸⁹])?(@[0-9A-Za-z]+)?))|(?:[\u2700-\u27bf]|(?:\ud83c[\udde6-\uddff]){2}|[\ud800-\udbff][\udc00-\udfff]|[\u0023-\u0039]\ufe0f?\u20e3|\u3299|\u3297|\u303d|\u3030|\u24c2|\ud83c[\udd70-\udd71]|\ud83c[\udd7e-\udd7f]|\ud83c\udd8e|\ud83c[\udd91-\udd9a]|\ud83c[\udde6-\uddff]|[\ud83c[\ude01-\ude02]|\ud83c\ude1a|\ud83c\ude2f|[\ud83c[\ude32-\ude3a]|[\ud83c[\ude50-\ude51]|\u203c|\u2049|[\u25aa-\u25ab]|\u25b6|\u25c0|[\u25fb-\u25fe]|\u00a9|\u00ae|\u2122|\u2139|\ud83c\udc04|[\u2600-\u26FF]|\u2b05|\u2b06|\u2b07|\u2b1b|\u2b1c|\u2b50|\u2b55|\u231a|\u231b|\u2328|\u23cf|[\u23e9-\u23f3]|[\u23f8-\u23fa]|\ud83c\udccf|\u2934|\u2935|[\u2190-\u21ff])|( ))*$/

    return !!emojiOrClockRegex.exec(this.props.post.message)
  }

  segmentFromMarkup(match) {
    return {
      type: "markup",
      text: match[2],
      tag: match[1],
      match: match
    };
  }

  segmentFromSpanMarkup(match) {
    var tag = "";
    switch (match[1]) {
      case "text-decoration: underline":
        tag = "u";
        break;
      case "text-decoration: line-through":
        tag = "s";
        break;
    }

    return {
      type: "markup",
      text: match[2],
      tag: tag,
      match: match
    };
  }

  segmentFromURL(match) {
    return {
      type: "url",
      text: "URL",
      url: match[1],
      match: match
    };
  }

  segmentFromClock(match) {
    return {
      type: "clock",
      text: match[11],
      match: match
    };
  }

  segmentFromText(text) {
    return {
      type: "text",
      text: text
    };
  }

  segments(text) {
    if (!text) {
      return [];
    }

    var segments = [];

    var patterns = [
      {
        pattern: /<(m|s|u|b|i|tt|code)>(.*?)<\/\1>/,
        f: this.segmentFromMarkup,
        recursive: true,
      },
      {
        pattern: /<span style="([^"]*)">(.*?)<\/span>/,
        f: this.segmentFromSpanMarkup,
        recursive: true,
      },
      {
        pattern: /<span style='([^']*)'>(.*?)<\/span>/,
        f: this.segmentFromSpanMarkup,
        recursive: true,
      },
      {
        pattern: /<a href="([^"]*)">[^<]*<\/a>/,
        f: this.segmentFromURL,
        recursive: false,
      },
      {
        pattern: /<a href='([^']*)'>[^<]*<\/a>/,
        f: this.segmentFromURL,
        recursive: false,
      },
      {
        pattern: /((([0-9]{4})-((0[1-9])|(1[0-2]))-((0[1-9])|([12][0-9])|(3[01])))[T #])?((([01]?[0-9])|(2[0-3])):([0-5][0-9])(:([0-5][0-9]))?([:\^][0-9]|[¹²³⁴⁵⁶⁷⁸⁹])?(@[0-9A-Za-z]+)?)/,
        f: this.segmentFromClock,
        recursive: false,
      }
    ];

    var remaining = text.substr(0);
    while (remaining.length > 0) {
      var matches = [];

      for (var i in patterns) {
        var pattern = patterns[i].pattern;

        var match = pattern.exec(remaining);

        if (match) {
          matches.push({
            pattern: patterns[i],
            match: match,
          });
        }
      }

      if (matches.length > 0) {
        var first_match = matches.reduce(function(prev, current) {
          if (!prev) {
            return current;
          }
          return (prev.match.index < current.match.index) ? prev : current
        });

        if (first_match.match.index > 0) {
          segments.push(this.segmentFromText(remaining.substr(0, first_match.match.index)));
          remaining = remaining.substr(first_match.match.index);
        }

        var segment = first_match.pattern.f(first_match.match);
        if (first_match.pattern.recursive) {
          segment.segments = this.segments(segment.text)
        }
        segments.push(segment);

        remaining = remaining.substr(first_match.match[0].length);
      } else {
        segments.push(this.segmentFromText(remaining));

        remaining = "";
      }
    }

    return segments;
  }

  renderSegment(segment, key) {
    if (segment.segments && segment.segments.length > 0) {
      var contents = segment.segments.map((segment, i) => this.renderSegment(segment, key + '.' + i))
    } else {
      var contents = segment.text
    }

    switch (segment.type) {
      case 'markup':
        var style = [styles.tribunePostMessageSegment];
        switch (segment.tag) {
          case 'b':
            style.push(styles.tribunePostMessageSegmentBold);
            break;
          case 'i':
            style.push(styles.tribunePostMessageSegmentItalic);
            break;
          case 'u':
            style.push(styles.tribunePostMessageSegmentUnderline);
            break;
          case 's':
            style.push(styles.tribunePostMessageSegmentStrikethrough);
            break;
        }
        return <Text key={key} style={style} text={segment.text}>{contents}</Text>
      case 'clock':
        return <PostMessageClock key={key} style={[styles.tribunePostMessageSegment, styles.tribunePostMessageSegmentClock]} text={contents} tribune={this.props.tribune} />
      case 'url':
        return <PostMessageURL key={key} style={[styles.tribunePostMessageSegment, styles.tribunePostMessageSegmentURL]} text={contents} url={segment.url} />
      case 'text':
        var style = [styles.tribunePostMessageSegment]
        if (this._hasOnlyEmojis) {
          style.push(styles.tribunePostMessageSegmentEmoji)
        }

        return <Text key={key} style={style}>{contents}</Text>
    }
  }

  renderedSegments() {
    return this.segments(this.props.post.message).map((segment, i) => this.renderSegment(segment, i));
  }

  render() {
    return (
      <View style={[styles.flip, styles.tribunePost]}>
        <TouchableHighlight style={styles.tribunePostInfoWrapper} onPress={this.appendClock}>
          <View style={styles.tribunePostInfo}>
            <Text numberOfLines={1} style={styles.tribunePostClock} selectable>{this.props.post.clock()}</Text>
            <Text numberOfLines={1} style={styles.tribunePostAuthor} selectable>{this.props.post.author()}</Text>
          </View>
        </TouchableHighlight>
        <TouchableHighlight style={styles.tribunePostMessageWrapper} underlayColor={'white'} activeOpacity={0.8} onPress={this.appendClock}>
          <View style={styles.tribunePostMessage}>
            <Text selectable>
              {this.renderedSegments()}
            </Text>
          </View>
        </TouchableHighlight>
      </View>
    );
  }
}

class PostMessageURL extends React.Component {
  constructor(props) {
    super(props);

    this.domain = this.props.url
      .replace(/https?:\/\//, '')
      .replace(/\/.*$/, '')
      .replace(/^www\./, '')
  }

  onPress = () => {
    Linking.openURL(this.props.url)
  }

  onLongPress = () => {
    Alert.alert(
      'URL',
      this.props.url,
      [
        {text: 'Open in browser', onPress: () => Linking.openURL(this.props.url)},
        {text: 'Copy in clipboard', onPress: () => Clipboard.setString(this.props.url)},
        {text: 'Share', onPress: () => Share.share({url: this.props.url, message: this.props.url})},
      ]
    )
  }

  render() {
    return <Text ref={(ref) => { this.text = ref }}
                onPress={this.onPress} onLongPress={this.onLongPress}
                style={this.props.style}>
             {this.domain}
           </Text>
  }
}

class PostMessageClock extends React.Component {
  render() {
    return <Text style={this.props.style}>{this.props.text}</Text>
  }
}



