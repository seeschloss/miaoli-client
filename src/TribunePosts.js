// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { FlatList, ScrollView } from 'react-native';


import { styles } from './style';

export class TribunePosts extends React.Component {
  constructor(props) {
    super(props);

    this.refreshing = false;

    this.state = {
      posts: []
    }
  }

  setRefreshing = (refreshing) => {
    this.refreshing = refreshing;

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

  renderScrollComponent = ({ style, refreshing, ...props }) => (
    <ScrollView keyboardShouldPersistTaps={'always'} style={[style, styles.flip]} />
  );

  appendClock = (clock) => {
    this.props.tribune.append(clock + " ")
  }

  renderItem = props => {
    const post = props.item.post

    return (<View style={[styles.flip, styles.tribunePost]}>
      <TouchableHighlight style={styles.tribunePostInfoWrapper} onPress={() => { this.appendClock(post.clock()) }}>
        <View style={styles.tribunePostInfo}>
          <Text numberOfLines={1} style={styles.tribunePostClock} selectable>{post.clock()}</Text>
          <Text numberOfLines={1} style={styles.tribunePostAuthor} selectable>{post.author()}</Text>
        </View>
      </TouchableHighlight>
      <PostMessage message={post.message} tribune={this.props.tribune} post={post} />
    </View>)
  };

  postsList() {
    return this.state.posts.map(p => { return { key: p.id, post: p } }).reverse()
  }

  render() {
    return (
      <FlatList
        contentContainerStyle={styles.tribunePosts}
        onRefresh={this.onRefresh}
        ref={(ref) => { this.flatList = ref }}
        extraData={this.state.posts}
        data={this.postsList()}
        renderItem={this.renderItem}
        renderScrollComponent={this.renderScrollComponent}
        />
    );
  }
}

import { TouchableHighlight, Text, View, Alert, Linking, Clipboard, Share } from 'react-native';
class PostMessage extends React.Component {
  appendClock = () => {
    this.props.tribune.append(this.props.post.clock() + " ");
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
      default:
        return <Text key={key} style={styles.tribunePostMessageSegment}>{contents}</Text>
    }
  }

  renderedSegments() {
    return this.segments(this.props.message).map((segment, i) => this.renderSegment(segment, i));
  }

  render() {
    return (
      <TouchableHighlight style={styles.tribunePostMessageWrapper} underlayColor={'white'} activeOpacity={0.8} onPress={this.appendClock}>
        <View style={styles.tribunePostMessage}>
          <Text selectable>
            {this.renderedSegments()}
          </Text>
        </View>
      </TouchableHighlight>
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



