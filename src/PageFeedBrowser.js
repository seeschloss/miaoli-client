// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableNativeFeedback, View, Text, Button, Picker } from 'react-native';

import BackgroundTimer from 'react-native-background-timer';

import { Icon } from 'react-native-elements';

import { styles } from './style';
import { Tribune } from './Tribune';
import { TribunePosts } from './TribunePosts';
import { TribuneInput } from './TribuneInput';

export class PageFeedBrowser extends React.Component {
  constructor(props) {
    super(props);

    this.tribunes = this.props.screenProps.tribunes
  }

  static navigationOptions = (navigation) => {
    var items = [
       <Picker.Item key="title" color={"grey"} label={navigation.screenProps.title} value="none" />,
       <Picker.Item key="settings" label="Settings" value="settings" />,
    ]
    return {
      title: navigation.screenProps.title,
      headerLeft: <View style={{marginLeft: 15}}>
                    <TouchableNativeFeedback
                      onPress={() => {navigation.screenProps.drawerNavigation.navigate('DrawerOpen')}}>
                        <Icon name="menu" />
                    </TouchableNativeFeedback>
                  </View>,
      headerRight: <View style={{width: 50, height: 50}}>
                     <TouchableNativeFeedback>
                       <Picker mode={'dropdown'} style={{backgroundColor: 'white', minWidth: 150}}
                           selectedValue={'none'}
                           onValueChange={(value) => {
                             switch (value) {
                               case 'settings':
                                 navigation.navigation.navigate('FeedSettings', {page: navigation.navigation.state.params.page})
                                 break;
                             }
                           }}>
                             {items}
                       </Picker>
                     </TouchableNativeFeedback>
                     <View pointerEvents="none" style={{width: 50, height: 50, backgroundColor: 'white', justifyContent: 'center', alignItems: 'center', position: 'absolute'}}>
                       <Icon name="more-vert" pointerEvents="none" />
                     </View>
                   </View>
    }
  }

  componentDidMount = () => {
    // This param will be used by PageFeedSettings to setState() when
    // values are changed.
    this.props.navigation.setParams({page: this})

    this.postsView.setPosts(this.allPosts())
  }

  componentWillUnmount() {
  }

  componentDidUpdate() {
  }

  append = (text) => {
    this.input.append(text);
  }

  post = (text) => {
    return this.tribunes[0].post(text);
  }

  allPosts = () => {
    var posts = []

    this.tribunes.forEach(tribune => {
      tribune.posts.forEach(post => {
        posts.push(post)
      })
    })

    posts.sort((a, b) => {
      if (a.time == b.time) {
        return a.id > b.id ? 1 : -1
      } else {
        return a.time > b.time ? 1 : -1
      }
    })

    console.log(posts.map(post => [ post.time, post.tribune.configuration.title ]).slice(-20))

    return posts
  }

  showNewPostButton = (show) => {
    this.input.setState({newPostButtonDisplay: show ? 'flex' : 'none'})
  }

  refreshTribune = () => {
    BackgroundTimer.clearTimeout(this.timeout);

    return Promise.all(this.tribunes.map(tribune => { console.log(['tribune', tribune.configuration.title]); return tribune.update() }))
      .then(posts => {
        if (this.postsView) {
          this.postsView.setPosts(this.allPosts())
        }

        this.timeout = BackgroundTimer.setTimeout(() => { this.refreshTribune() }, 30000)

        posts.forEach(p => {
          console.log(['posts', p.length])
        })
      })
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <View style={styles.tribune}>
          <TribunePosts ref={(ref) => { this.postsView = ref; }} tribune={this} />
          <TribuneInput ref={(ref) => { this.input = ref; }} tribune={this} />
        </View>
      </View>
    );
  }
}

