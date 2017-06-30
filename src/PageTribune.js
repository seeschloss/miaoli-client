// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text, Button } from 'react-native';

import { StackNavigator } from 'react-navigation';

import { styles } from './style';
import { Tribune } from './Tribune';
import { PageTribuneSettings } from './PageTribuneSettings';


class PageTribuneBrowser extends React.Component {
  static navigationOptions = (navigation) => {
    const tribuneConfiguration = navigation.screenProps.tribune

    return {
      title: tribuneConfiguration.title,
      headerRight: <Button title="Settings" onPress={() => {
                      navigation.navigation.navigate('TribuneSettings', {tribune: tribuneConfiguration})
                      }}
                   />
    }
  }

  render() {
    console.log(['tribune', this.props.screenProps]);
    return (
      <View style={styles.tribuneContainer}>
        <Tribune key={this.props.screenProps.tribuneId} configuration={this.props.screenProps.tribune} />
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
    screen: PageTribuneSettings,
    path: 'tribune/:tribune/settings',
  },
});

export class PageTribune extends React.Component {
  static navigationOptions = ({navigation, screenProps}) => {
    const tribune = navigation.state.key.replace(/^tribune-/, '')
    const configuration = screenProps.configuration[tribune];

    return {
      title: configuration.title,
    }
  }

  render() {
    const tribune = this.props.navigation.state.key.replace(/^tribune-/, '')
    const configuration = this.props.screenProps.configuration[tribune];

    return (
       <NavigationStack screenProps={{tribune: configuration, tribuneId: tribune}} />
    );
  }
}

