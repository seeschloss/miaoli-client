// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text, Button } from 'react-native';

import { StackNavigator } from 'react-navigation';

import { styles } from './style';
import { Tribune } from './Tribune';
import { TribunePosts } from './TribunePosts';
import { TribuneInput } from './TribuneInput';

export class PageTribuneBrowser extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      posts: []
    }

    this.configuration = this.props.screenProps.tribune
    this.tribune = new Tribune(this.configuration)
  }

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

  componentDidMount() {
    setTimeout(() => { this.refreshTribune() }, 1000)
    this.interval = setInterval(() => { this.refreshTribune() }, 10000)
  }

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  componentDidUpdate() {
  }

  append = (text) => {
    this.input.append(text);
  }

  post = (text) => {
    return this.tribune.post(text)
      .then(() => this.refreshTribune())
  }

  refreshTribune = () => {
    return this.tribune.update()
      .then(posts => this.update(posts))
  }

  update = (posts) => {
    this.postsView.setRefreshing(true);
    this.setState({posts: posts})
    this.postsView.setState({posts: posts})
    this.postsView.setRefreshing(false);
  }

  render() {
    return (
      <View style={styles.tribuneContainer}>
        <View style={styles.tribune}>
          <TribunePosts ref={(ref) => { this.postsView = ref; }} tribune={this} />
          <TribuneInput ref={(ref) => { this.input = ref; }} tribune={this} />
        </View>
      </View>
    );
  }
}

