// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { View, SectionList, Button, Text, TouchableNativeFeedback, Modal, TextInput, AsyncStorage, WebView } from 'react-native';
import CookieManager from 'react-native-cookies';

import { styles } from './style';
import { Tribune } from './Tribune';

export class PageTribuneLogin extends React.Component {
  static navigationOptions = ({navigation, screenProps}) => ({
    title: screenProps.tribune.configuration.title + ' login',
  })

  onNavigationStateChange = (e) => {
    CookieManager.get(this.props.screenProps.tribune.configuration.loginpage, (err, res) => {
      var cookie = "";
      for (var key in res) {
        cookie += key + "=" + res[key] + "&"
      }

      this.props.screenProps.tribune.configuration.cookie = cookie
    })
  }

  render() {
    return (
      <WebView
        source={{uri: this.props.screenProps.tribune.configuration.loginpage}}
        onNavigationStateChange={this.onNavigationStateChange}
        />
    );
  }
}

