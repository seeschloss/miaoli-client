// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text } from 'react-native';

import { styles } from './style';
import { Tribune } from './Tribune';

export class PageSettings extends React.Component {
  static navigationOptions = {
    title: 'Settings',
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <Text>Settings</Text>
      </View>
    );
  }
}

