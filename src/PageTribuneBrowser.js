// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableNativeFeedback, View, Text, Button, Picker } from 'react-native';

import { StackNavigator } from 'react-navigation';
import { Icon } from 'react-native-elements';

import { styles } from './style';
import { Tribune } from './Tribune';
import { TribunePosts } from './TribunePosts';
import { TribuneInput } from './TribuneInput';

export class PageTribuneBrowser extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      posts: []
    }

    this.configuration = this.props.screenProps.tribune
    this.tribune = new Tribune(this.configuration)
  }

  static navigationOptions = (navigation) => {
    const tribuneConfiguration = navigation.screenProps.tribune

    return {
      title: tribuneConfiguration.title,
      headerLeft: <View style={{marginLeft: 15}}><TouchableNativeFeedback onPress={() => {navigation.screenProps.drawerNavigation.navigate('DrawerOpen')}}><Icon name="menu" /></TouchableNativeFeedback></View>,
      headerRight: <View style={{width: 50, height: 50}}>
                     <TouchableNativeFeedback>
                       <Picker mode={'dropdown'} style={{backgroundColor: 'white'}}
                           selectedValue={'none'}
                           onValueChange={(value) => {
                             switch (value) {
                               case 'settings':
                                 navigation.navigation.navigate('TribuneSettings', {tribune: tribuneConfiguration})
                                 break;
                             }
                           }}>
                         <Picker.Item color={"grey"} label={tribuneConfiguration.title} value="none" />
                         <Picker.Item label="Settings" value="settings" />
                       </Picker>
                     </TouchableNativeFeedback>
                     <View style={{width: 50, height: 50, justifyContent: 'center', alignItems: 'center', position: 'absolute'}}>
                       <Icon name="more-vert" pointerEvents="none" />
                     </View>
                   </View>
    }
  }

  componentDidMount() {
    setTimeout(() => { this.refreshTribune() }, 1000)
  }

  componentWillUnmount() {
    clearTimeout(this.timeout);
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
    clearTimeout(this.timeout);
    return this.tribune.update()
      .then(posts => {
        this.postsView.setPosts(posts)
        this.timeout = setTimeout(() => { this.refreshTribune() }, 30000)
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

