// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TouchableHighlight, Text, View, Alert, Linking, Clipboard, Share } from 'react-native';

import { styles } from './style';

export class Post extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      id: 0,
      time: "",
      info: "",
      login: "",
      message: "",
    }
  }

  keyExtractor() {
    return this.state.id;
  }

  clock() {
    if (this.props.time == undefined) {
      console.log(this);
      return;
    }
    var year = this.props.time.substr(0, 4);
    var month = this.props.time.substr(4, 2);
    var day = this.props.time.substr(6, 2);
    var hour = this.props.time.substr(8, 2);
    var minute = this.props.time.substr(10, 2);
    var second = this.props.time.substr(12, 2);

    return hour + ":" + minute + ":" + second;
  }

  author() {
    return this.props.login ? this.props.login : this.props.info;
  }

  message() {
    return this.props.message;
  }

  appendClock = () => {
    this.props.tribune.append(this.clock() + " ");
  }

  render() {
    return (
      <View style={[styles.flip, styles.tribunePost]}>
        <TouchableHighlight style={styles.tribunePostInfoWrapper} onPress={this.appendClock}>
          <View style={styles.tribunePostInfo}>
            <Text numberOfLines={1} style={styles.tribunePostClock} selectable>{this.clock()}</Text>
            <Text numberOfLines={1} style={styles.tribunePostAuthor} selectable>{this.author()}</Text>
          </View>
        </TouchableHighlight>
        <PostMessage message={this.message()} tribune={this.props.tribune} post={this} />
      </View>
    );
  }
}

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

  segments() {
    if (!this.props.message) {
      return [];
    }

    var segments = [];

    var patterns = [
      {
        pattern: /<(m|s|u|b|i|tt|code)>(.*?)<\/\1>/,
        f: this.segmentFromMarkup
      },
      {
        pattern: /<span style="([^"]*)">(.*?)<\/span>/,
        f: this.segmentFromSpanMarkup
      },
      {
        pattern: /<span style='([^']*)'>(.*?)<\/span>/,
        f: this.segmentFromSpanMarkup
      },
      {
        pattern: /<a href="([^"]*)">[^<]*<\/a>/,
        f: this.segmentFromURL
      },
      {
        pattern: /<a href='([^']*)'>[^<]*<\/a>/,
        f: this.segmentFromURL
      },
      {
        pattern: /((([0-9]{4})-((0[1-9])|(1[0-2]))-((0[1-9])|([12][0-9])|(3[01])))[T #])?((([01]?[0-9])|(2[0-3])):([0-5][0-9])(:([0-5][0-9]))?([:\^][0-9]|[¹²³⁴⁵⁶⁷⁸⁹])?(@[0-9A-Za-z]+)?)/,
        f: this.segmentFromClock
      }
    ];

    var remaining = this.props.message.substr(0);
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

        segments.push(first_match.pattern.f(first_match.match));

        remaining = remaining.substr(first_match.match[0].length);
      } else {
        segments.push(this.segmentFromText(remaining));

        remaining = "";
      }
    }

    return segments;
  }

  renderedSegments() {
    return this.segments().map((segment, i) => {
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
            return <Text key={i} style={style} text={segment.text}>{segment.text}</Text>
          case 'clock':
            return <PostMessageClock key={i} style={[styles.tribunePostMessageSegment, styles.tribunePostMessageSegmentClock]} text={segment.text} tribune={this.props.tribune} />
          case 'url':
            return <PostMessageURL key={i} style={[styles.tribunePostMessageSegment, styles.tribunePostMessageSegmentURL]} text={segment.text} url={segment.url} />
          default:
            return <Text key={i} style={styles.tribunePostMessageSegment}>{segment.text}</Text>
        }
    });
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

