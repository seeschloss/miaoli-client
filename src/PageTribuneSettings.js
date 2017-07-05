// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { View, SectionList, Button, Text, TouchableNativeFeedback, Modal, TextInput } from 'react-native';

import { styles } from './style';
import { Tribune } from './Tribune';

export class PageTribuneSettings extends React.Component {
  static navigationOptions = ({navigation, screenProps}) => ({
    title: screenProps.tribune.title + ' settings',
  })

  render() {
    return (
      <SectionList style={{flex: 1, backgroundColor: 'white'}}
        renderItem={({item}) => <ListItem title={item.title} settingKey={item.key} value={item.value} />}
        renderSectionHeader={({section}) => <ListHeader title={section.title} key={section.key} />}
        sections={[
          {data: [
            {title: 'Display name', key: "user_agent", value: this.props.screenProps.tribune.user_agent},
            {title: 'Color', key: "color", value: this.props.screenProps.tribune.color},
            {title: 'Backend', key: "backend", value: this.props.screenProps.tribune.backend},
            {title: 'Post URL', key: "post_url", value: this.props.screenProps.tribune.post_url},
            {title: 'Post format', key: "post_format", value: this.props.screenProps.tribune.post_format},
          ], title: "Tribune settings", key: "tribune-settings"},
        ]}
      />
    );
  }
}

class ListItem extends React.Component {
  constructor(props) {
    super(props)

    this.state = {
      modalVisible: false,
      value: this.props.value,
    }
  }

  onPress = () => {
    this.initialValue = this.state.value
    this.setState({modalVisible: true})
  }

  changeText = (text) => {
    this.currentText = text;
  }

  changeValue = () => {
    this.setState({value: this.currentText})
    this.setState({modalVisible: false})
  }

  render() {
    return (
      <TouchableNativeFeedback onPress={this.onPress}>
        <View style={styles.menuItem}>
          <Modal
              transparent={false}
              visible={this.state.modalVisible}
              onRequestClose={this.changeValue}
              >
            <View style={styles.menuModal}>
              <Text style={styles.menuItemLabel}>{this.props.title}</Text>
              <Text style={[styles.menuItemValue, {marginLeft: 20, marginBottom: 50}]}>{this.initialValue}</Text>
              <TextInput
                  onSubmitEditing={this.changeValue}
                  defaultValue={this.state.value}
                  onChangeText={text => this.changeText(text)}
                  />
              <Button title="OK" onPress={this.changeValue} />
            </View>
          </Modal>
          <Text style={styles.menuItemLabel}>{this.props.title}</Text>
          <Text style={styles.menuItemValue} numberOfLines={1} ellipsizeMode={'middle'}>{this.state.value}</Text>
        </View>
      </TouchableNativeFeedback>
    );
  }
}

class ListHeader extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <View style={styles.menuHeader}>
        <Text>{this.props.title}</Text>
      </View>
    );
  }
}

