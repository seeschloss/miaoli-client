// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text, Button } from 'react-native';

import { StackNavigator } from 'react-navigation';

import { styles } from './style';
import { Tribune } from './Tribune';
import { PageSettings } from './PageSettings';


class PageTribuneBrowser extends React.Component {
  static navigationOptions = (navigation) => {
    const tribune = navigation.screenProps.tribune

    return {
      title: tribune,
      headerRight: <Button title="Settings" onPress={() => {
                      navigation.navigation.navigate('TribuneSettings', {tribuneTitle: tribune})
                      }}
                   />
    }
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <Tribune />
      </View>
    );
  }
}

const NavigationStack = StackNavigator({
  TribuneHome: {
    screen: PageTribuneBrowser,
    path: 'tribune/:tribuneTitle',
  },
  TribuneSettings: {
    screen: PageSettings,
    path: 'tribune/:tribuneTitle/settings',
  },
});

export class PageTribune extends React.Component {
  render() {
    const tribune = this.props.navigation.state.key.replace(/^Tribune-/, '')
    return (
       <NavigationStack screenProps={{tribune: tribune}} />
    );
  }
}

