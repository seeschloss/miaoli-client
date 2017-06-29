// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text } from 'react-native';

import { styles } from './style';
import { Tribune } from './Tribune';

export class PageTribune extends React.Component {
  static navigationOptions = {
    title: 'Moules',
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <Tribune />
      </View>
    );
  }
}

