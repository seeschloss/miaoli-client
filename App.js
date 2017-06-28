// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar } from 'react-native';

import { styles } from './src/style';
import { Tribune } from './src/tribune';

export default class App extends React.Component {
  componentWillMount() {
    setTimeout(() => {
      StatusBar.setHidden(false);
      StatusBar.setTranslucent(false);
      StatusBar.setBarStyle("light-content");
      StatusBar.setBackgroundColor("black");
    }, 1000);
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <Tribune title='Public' />
      </View>
    );
  }
}
