import React from 'react';
import { Text, View } from 'react-native';

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
        .replace(/&quot;/g, '"')

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

