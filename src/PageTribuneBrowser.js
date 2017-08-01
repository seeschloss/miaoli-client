// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableNativeFeedback, View, Text, Button, Picker, Keyboard } from 'react-native';

import BackgroundTimer from 'react-native-background-timer';

import { Icon } from 'react-native-elements';

import { styles } from './style';
import { Tribune } from './Tribune';
import { TribunePosts } from './TribunePosts';
import { TribuneInput } from './TribuneInput';

export class PageTribuneBrowser extends React.Component {
  constructor(props) {
    super(props);

    this.tribune = this.props.screenProps.tribune
    this.configuration = this.tribune.configuration
  }

  static navigationOptions = (navigation) => {
    const tribuneConfiguration = navigation.screenProps.tribune.configuration

    var items = [
       <Picker.Item key="title" color={"grey"} label={tribuneConfiguration.title} value="none" />,
       <Picker.Item key="settings" label="Settings" value="settings" />,
    ]

    if (tribuneConfiguration.loginpage) {
      items.push(
       <Picker.Item key="login"    label="Login" value="login" style={{display: 'none'}} />
      )
    }

    return {
      title: tribuneConfiguration.title,
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
                                 navigation.navigation.navigate('TribuneSettings', {tribune: navigation.screenProps.tribune, page: navigation.navigation.state.params.page})
                                 break;
                               case 'login':
                                 navigation.navigation.navigate('TribuneLogin', {tribune: navigation.screenProps.tribune})
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

  componentDidMount() {
    // This param will be used by PageTribuneSettings to setState() when
    // values are changed.
    this.props.navigation.setParams({page: this})


    BackgroundTimer.clearTimeout(this.timeout);
    this.timeout = BackgroundTimer.setTimeout(() => { this.refreshTribune() }, 1000)
  }

  componentWillUnmount() {
    //BackgroundTimer.clearTimeout(this.timeout);
  }

  componentDidUpdate() {
  }

  append = (text) => {
    this.input.append(text);
  }

  post = (text) => {
    return this.tribune.post(text)
      .then(() => this.refreshTribune())
  }

  showNewPostButton = (show) => {
    this.input.setState({newPostButtonDisplay: show ? 'flex' : 'none'})
  }

  refreshTribune = () => {
    BackgroundTimer.clearTimeout(this.timeout);
    return this.tribune.update()
      .then(posts => {
        if (this.postsView) {
          this.postsView.setPosts(posts)
        }

        this.timeout = BackgroundTimer.setTimeout(() => { this.refreshTribune() }, 30000)
        console.log(['posts', this.tribune.configuration.title, posts.length])
      })
  }

  onPostMessage = (text) => {
    this.post(text)
      .then(posts => {
        this.input.clear()
        Keyboard.dismiss()
        this.refreshTribune()
      })
  }

  onClickPost = (post) => {
    this.append(post.clock() + " ")
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <View style={styles.tribune}>
          <TribunePosts ref={(ref) => { this.postsView = ref; }} onClickPost={this.onClickPost} tribuneView={this} />
          <TribuneInput ref={(ref) => { this.input = ref; }} onPostMessage={ this.onPostMessage } />
        </View>
      </View>
    );
  }
}

