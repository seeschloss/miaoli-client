// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { Text, View, Keyboard } from 'react-native';

var DOMParser = require('xmldom').DOMParser;

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

    this.backend = this.props.configuration.backend
    this.backend_type = this.props.configuration.backend_type
    this.post_url = this.props.configuration.post_url
    this.post_format = this.props.configuration.post_format
    this.user_agent = this.props.configuration.user_agent

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
      .then(response => {
        var backendContentType = response.headers.get('Content-Type')
        response.text().then(text => {
          if (backendContentType && backendContentType.match('text/tab-separated-values')) {
            var posts = this.parseTsv(text)
          } else {
            var posts = this.parseXml(text)
          }

          if (this._isMounted) {
            this.setState({posts: posts})
          }

          if (callback) {
            callback(posts)
          }

          if (this.postsView) {
            this.postsView.setRefreshing(false);
          }
        })
        .done()
      })
      .done()
  }

  postsList() {
    return this.state.posts.map(p => { return { key: p.props.id, post: p } })
  }

  parseXml(xml) {
    var dom = new DOMParser().parseFromString(xml)

    var xmlPosts = dom.getElementsByTagName('post')

    var posts = [];

    for (var i = 0; i < xmlPosts.length; i++) {
      const xmlPost = xmlPosts.item(i)

      const id = xmlPost.getAttribute('id')
      const time = xmlPost.getAttribute('time')
      const infoElement = xmlPost.getElementsByTagName('info')
      var info = infoElement.length ? infoElement.item(0).textContent : ""

      const loginElement = xmlPost.getElementsByTagName('login')
      var login = loginElement.length ? loginElement.item(0).textContent : ""

      const messageElement = xmlPost.getElementsByTagName('message')

      if (messageElement.length > 0) {
        if (messageElement.item(0).hasChildNodes()) {
          var message = "";

          for (var j = 0; j < messageElement.item(0).childNodes.length; j++) {
            var childNode = messageElement.item(0).childNodes.item(j);

            switch (childNode.tagName) {
              case "a":
                message += '<a href="' + childNode.getAttribute('href') + '">' + childNode.textContent + '</a>';
                break;
              case undefined:
                if (this.backend_type == "xml-htmlentitised") {
                  message += childNode.textContent
                    .replace(/&gt;/g, '>')
                    .replace(/&lt;/g, '<')
                    .replace(/&amp;/g, '&')
                    .replace(/&quot;/g, '"')
                } else {
                  message += childNode.textContent;
                }
                break;
              default:
                message += '<' + childNode.tagName + '>' + childNode.textContent + '</' + childNode.tagName + '>';
                break;
            }
          }
        } else {
          var message = messageElement.length ? messageElement.item(0).textContent : ""
        }
      } else {
        var message = "";
      }

      posts.push(
        <Post id={id} time={time} info={info} login={login} message={message} tribune={this} />
      )
    }

    return posts.reverse()
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

    return posts
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

