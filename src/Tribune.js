// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { Text, View, Keyboard } from 'react-native';

import { styles } from './style';

import { Post } from './Post';
import { TribunePosts } from './TribunePosts';
import { TribuneInput } from './TribuneInput';

export class Tribune extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      posts: []
    }

    this.backend = 'http://moules.org/board/last.php?backend=tsv';
    this.post_url = 'http://moules.org/board/add.php';
    this.post_format = 'message=%s';
    this.user_agent = 'Miaoli/0.0';

  }

  componentDidMount() {
    setTimeout(() => { this.update() }, 1000)
    this.interval = setInterval(() => { this.update() }, 10000)
    this._isMounted = true
  }

  componentWillUnmount() {
    this._isMounted = false
    clearInterval(this.interval);
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
        if (response.ok) {
          this.update((posts) => {
            this.input.clear();
            Keyboard.dismiss();
          });
        }
      })
  }

  update(callback) {
    if (this.postsView) {
      this.postsView.setRefreshing(true);
    }

    fetch(this.backend)
      .then(response => response.text())
      .then(tsv => { this.parseTsv(tsv, callback); })
  }

  postsList() {
    return this.state.posts.map(p => { return { key: p.props.id, post: p } })
  }

  parseTsv(tsv, callback) {
    var posts = tsv.split(/\n/).map(line => line.split(/\t/));

    posts = posts.filter(post => post[0] > 0).map(post => {
      var message = post[4]
        .replace(/&gt;/g, '>')
        .replace(/&lt;/g, '<')
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')

      return <Post id={post[0]} time={post[1]} info={post[2]} login={post[3]} message={message} tribune={this} />
    });

    if (this._isMounted) {
      this.setState({posts: posts})
    }

    if (this.postsView) {
      this.postsView.setRefreshing(false);
    }

    if (callback) {
      callback(posts)
    }
  }

  onTextChange(text) {
    return text
  }

  render() {
    return (
      <View style={styles.tribune}>
        <TribunePosts ref={(ref) => { this.postsView = ref; }} tribune={this} />
        <TribuneInput ref={(ref) => { this.input = ref; }} tribune={this} />
      </View>
    );
  }
}

