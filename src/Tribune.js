// vim: et ts=2 sts=2 sw=2

var DOMParser = require('xmldom').DOMParser;

import { Post } from './Post';

export class Tribune {
  constructor(configuration) {
    this.backend = configuration.backend
    this.backend_type = configuration.backend_type
    this.post_url = configuration.post_url
    this.post_format = configuration.post_format
    this.user_agent = configuration.user_agent

  }

  post = (text) => {
    if (text.length == 0) {
      return this.update();
    }

    var headers = new Headers();
    headers.append('Content-Type', 'application/x-www-form-urlencoded');
    headers.append('User-Agent', this.user_agent);

    return fetch(this.post_url, {
      method: 'POST',
      headers: headers,
      body: this.post_format.replace('%s', text)
    }).then(() => { console.log(['posted']) })
  }

  update() {
    return fetch(this.backend)
      .then(response => {
        var backendContentType = response.headers.get('Content-Type')
        return response.text().then(text => {
          if (backendContentType && backendContentType.match('text/tab-separated-values')) {
            var posts = this.parseTsv(text)
          } else {
            var posts = this.parseXml(text)
          }

          return posts
        })
      })
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
        new Post({id: id, time: time, info: info, login: login, message: message, tribune: this})
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

      return new Post({id: post[0], time: post[1], info: post[2], login: post[3], message: message, tribune: this})
    });

    return posts
  }
}

