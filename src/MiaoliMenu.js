// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { View, Text } from 'react-native';

import { styles } from './style';

export class MiaoliMenu extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <View style={{flex: 1, backgroundColor: '#fff'}}>
        <Text style={{margin: 10, fontSize: 15, textAlign: 'left'}}>I'm in the Drawer!</Text>
      </View>
    );
  }
}


