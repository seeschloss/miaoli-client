// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { StyleSheet,
  Alert, Share, Clipboard, Linking, Keyboard,
  StatusBar, Button, Text, TextInput, View, FlatList, ScrollView } from 'react-native';

class TribuneInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      text: "",
    }
  }
  postMessage = () => {
    this.props.tribune.post(this.state.text)
  }

  onTextChange = (text) => {
    this.setState({text: text})
  }

  clear = () => {
    this.input.clear();
    this.setState({text: ""})
  }

  append = (text) => {
    this.setState({text: this.state.text + text})
    this.input.focus();
  }

  render() {
    return (
      <View style={styles.tribuneInput}>
        <TextInput ref={(ref) => { this.input = ref }} style={styles.tribuneInputText} placeholder='Your message' onChangeText={text => this.onTextChange(text)} value={this.state.text} />
        <Button ref={(ref) => { this.button = ref }} style={styles.tribuneInputButton} onPress={this.postMessage} title='✉️' />
      </View>
    );
  }
}

class TribunePosts extends React.Component {
  constructor(props) {
    super(props);

    this.refreshing = false;
  }

  setRefreshing = (refreshing) => {
    this.refreshing = refreshing;

    this.flatList.refreshing = refreshing;

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
    <ScrollView style={[style, styles.flip]} />
  );

  renderItem = props => (
      props.item.post
  );

  onRefresh = () => {
    this.props.tribune.update()
  }

  render() {
    return (
      <FlatList
        contentContainerStyle={styles.tribunePosts}
        onRefresh={this.onRefresh}
        ref={(ref) => { this.flatList = ref }}
        extraData={this.props.tribune.state.posts}
        data={this.props.tribune.postsList().reverse()}
        renderItem={this.renderItem}
        renderScrollComponent={this.renderScrollComponent}
        />
    );
  }
}

class Post extends React.Component {
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
        <View style={styles.tribunePostInfo}>
          <Text numberOfLines={1} onPress={this.appendClock} style={styles.tribunePostClock} selectable>{this.clock()}</Text>
          <Text numberOfLines={1} onPress={this.appendClock} style={styles.tribunePostAuthor} selectable>{this.author()}</Text>
        </View>
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
      text: "[url]",
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
            return <Text onPress={this.appendClock} key={i} style={style} text={segment.text}>{segment.text}</Text>
          case 'clock':
            return <PostMessageClock key={i} style={[styles.tribunePostMessageSegment, styles.tribunePostMessageSegmentClock]} text={segment.text} tribune={this.props.tribune} />
          case 'url':
            return <PostMessageURL key={i} style={[styles.tribunePostMessageSegment, styles.tribunePostMessageSegmentURL]} text={segment.text} url={segment.url} />
          default:
            return <Text onPress={this.appendClock} key={i} style={styles.tribunePostMessageSegment}>{segment.text}</Text>
        }
    });
  }

  render() {
    return (
      <View style={styles.tribunePostMessage}>
        <Text selectable>{this.renderedSegments()}</Text>
      </View>
    );
  }
}

class PostMessageURL extends React.Component {
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
    return <Text pressRetentionOffset={{top: 20, bottom: 20, right: 20, left: 20}} style={this.props.style} onPress={this.onPress} onLongPress={this.onLongPress}>{this.props.text}</Text>
  }
}

class PostMessageClock extends React.Component {
  render() {
    return <Text style={this.props.style}>{this.props.text}</Text>
  }
}

class Tribune extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      posts: []
    }

    this.backend = 'http://moules.org/board/last.php?backend=tsv';
    this.post_url = 'http://moules.org/board/add.php';
    this.post_format = 'message=%s';
    this.user_agent = 'Miaoli/0.0';

    setTimeout(() => { this.update() }, 1000)
    setInterval(() => { this.update() }, 10000)
  }

  append = (text) => {
    this.input.append(text);
  }

  post = (text) => {
    if (text.length == 0) {
      return this.update();
    }

    var headers = new Headers();
    headers.append('Content-Type', 'application/x-www-form-urlencoded');
    headers.append('User-Agent', this.user_agent);

    fetch(this.post_url, {
        method: 'POST',
        headers: headers,
        body: this.post_format.replace('%s', text)
      })
      .then(response => {
        console.log(response);
        if (response.ok) {
          this.input.clear();
          Keyboard.dismiss();

          this.update();
        }
      })
  }

  update() {
    this.postsView.setRefreshing(true);
    fetch(this.backend)
      .then(response => response.text())
      .then(tsv => { this.parseTsv(tsv); })
  }

  postsList() {
    return this.state.posts.map(p => { return { key: p.props.id, post: p } })
  }

  parseTsv(tsv) {
    var posts = tsv.split(/\n/).map(line => line.split(/\t/));

    posts = posts.filter(post => post[0] > 0).map(post => {
      var message = post[4]
        .replace(/&gt;/g, '>')
        .replace(/&lt;/g, '<')
        .replace(/&amp;/g, '&')

      return <Post id={post[0]} time={post[1]} info={post[2]} login={post[3]} message={message} tribune={this} />
    });

    this.setState({posts: posts});
    this.postsView.setRefreshing(false);
  }

  onTextChange(text) {
    return text
  }

  render() {
    return (
      <View style={styles.tribune}>
        <Text style={styles.tribuneTitle}>{this.props.title}</Text>
        <TribunePosts ref={(ref) => { this.postsView = ref; }} tribune={this} />
        <TribuneInput ref={(ref) => { this.input = ref; }} tribune={this} />
      </View>
    );
  }
}

export default class App extends React.Component {
  componentWillMount() {
    setTimeout(() => {
      StatusBar.setHidden(false);
      StatusBar.setTranslucent(false);
      StatusBar.setBarStyle("light-content");
      StatusBar.setBackgroundColor("black");
    }, 1000);
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <Tribune title='Public' />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  tribuneContainer: {
    flex: 1,
    backgroundColor: 'black',
  },
  tribune: {
    backgroundColor: 'white',
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'space-around',
    alignItems: 'stretch',
    padding: 5,
  },
  tribuneTitle: {
    flex: 0,
  },
  tribunePosts: {
    flex: 1,
    justifyContent: 'flex-start',
  },
  tribunePost: {
    flex: -1,
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
    paddingRight: 5,
    borderRadius: 10,
    backgroundColor: '#CDDC39',
    borderColor: '#CDDC39',
    borderWidth: 1,
  },
  tribunePostInfo: {
    flex: 0,
    flexDirection: 'column',
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
    alignSelf: 'stretch',
    marginRight: 4,
    backgroundColor: 'white',
    borderColor: 'white',
    paddingTop: 3,
    paddingBottom: 3,
    paddingLeft: 3,
    paddingRight: 3,
    borderTopLeftRadius: 10,
    borderBottomLeftRadius: 10,
    borderWidth: 0,
  },
  tribunePostClock: {
    flex: 0,
    fontWeight: 'bold',
  },
  tribunePostAuthor: {
    flex: 0,
    color: 'green',
    width: 50,
    overflow: 'hidden',
  },
  tribunePostMessage: {
    flex: 1,
  },
  tribunePostMessageSegment: {
    color: 'black',
  },
  tribunePostMessageSegmentURL: {
    fontWeight: 'bold',
  },
  tribunePostMessageSegmentClock: {
    color: 'blue',
  },
  tribunePostMessageSegmentBold: {
    fontWeight: 'bold',
  },
  tribunePostMessageSegmentItalic: {
    fontStyle: 'italic',
  },
  tribunePostMessageSegmentStrikethrough: {
    textDecorationLine: 'line-through',
  },
  tribunePostMessageSegmentUnderline: {
    textDecorationLine: 'underline',
  },
  tribuneInput: {
    flex: 0,
    flexDirection: 'row',
    justifyContent: 'space-around'
  },
  tribuneInputText: {
    flex: 1,
  },
  tribuneInputButton: {
    width: 200,
    height: 200,
  },
  flip: {
    transform: [{ scaleY: -1 }],
  },
});
