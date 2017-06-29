// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { ScrollView, TouchableNativeFeedback, Text } from 'react-native';
import { NavigationActions } from 'react-navigation'

import { styles } from './style';

export class MiaoliMenu extends React.Component {
  constructor(props) {
    super(props);
  }

  showSettings = () => {
    const navigateAction = NavigationActions.navigate({
      routeName: 'Settings'
    });

    this.props.navigation.dispatch(navigateAction)
    this.props.drawer.closeDrawer()
  }

  render() {
    return (
      <ScrollView style={{flex: 1, backgroundColor: '#fff'}}>
        <TouchableNativeFeedback onPress={this.showSettings}>
          <Text style={{padding: 10, fontSize: 15, textAlign: 'left'}}>Settings</Text>
        </TouchableNativeFeedback>
      </ScrollView>
    );
  }
}


