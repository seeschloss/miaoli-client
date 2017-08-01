// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { View, FlatList, Text, TouchableNativeFeedback } from 'react-native';

import BackgroundTimer from 'react-native-background-timer';

import { Button, List, ListItem, Icon } from 'react-native-elements';

import { styles } from './style';
import { Tribune } from './Tribune';
import { Settings } from './Settings';

export class PageNewTribuneType extends React.Component {
  constructor(props) {
    super(props);
  }

  static navigationOptions = (navigation) => {
    return {
      title: "Tribune type",
      headerLeft: <View style={{marginLeft: 15}}>
                    <TouchableNativeFeedback
                      onPress={() => {navigation.screenProps.drawerNavigation.navigate('DrawerOpen')}}>
                        <Icon name="menu" />
                    </TouchableNativeFeedback>
                  </View>,
    }
  }

  render() {
    return (
      <View>
        <Button title="Predefined settings" 
          onPress={() => {this.props.navigation.navigate('TribuneChoosePredefined')}}
          />
        <Button title="Manual settings"
          onPress={() => {this.props.navigation.navigate('TribuneDetails')}}
          />
      </View>
    );
  }
}

export class PageNewTribunePredefined extends React.Component {
  constructor(props) {
    super(props);
  }

  static navigationOptions = (navigation) => {
    return {
      title: "Predefined tribunes",
    }
  }

  renderItem = props => {
    return (
      <Text>{props.item.title}</Text>
    )
  };

  onPress = (item) => {
    console.log(['press', item])
    Settings.activateTribune(item)
  }

  render = () => {
    return (
      <List>
        {
          Object.values(Settings.knownTribunes).map((tribune, i) => (
            <ListItem
              key={tribune.id}
              title={tribune.title}
              onPress={ (item) => this.onPress(tribune) }
              />
          ))
        }
      </List>
    )
  }
}

export class PageNewTribuneDetails extends React.Component {
  constructor(props) {
    super(props);
  }

  static navigationOptions = (navigation) => {
    return {
      title: "Tribune details",
    }
  }

  render() {
    return (
      <View>
        <Button title="Predefined settings" />
        <Button title="Manual settings" />
      </View>
    );
  }
}

