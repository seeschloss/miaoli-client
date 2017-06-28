// vim: et ts=2 sts=2 sw=2
import React from 'react';
import { View, StatusBar, DrawerLayoutAndroid, Text } from 'react-native';

import { styles } from './src/style';
import { MiaoliMenu, Tribune } from './src/tribune';

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
    var navigationView = ( <MiaoliMenu /> );

    return (
      <DrawerLayoutAndroid
        drawerWidth={300}
        drawerPosition={DrawerLayoutAndroid.positions.Left}
        renderNavigationView={() => navigationView}>
          <View style={styles.tribuneContainer}>
            <Tribune title='Public' />
          </View>
        </DrawerLayoutAndroid>
    );
  }
}
