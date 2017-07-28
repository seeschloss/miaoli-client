// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { View, SectionList, Button, Text, TouchableNativeFeedback, Modal, TextInput, Picker, AsyncStorage } from 'react-native';

import { styles } from './style';
import { Tribune } from './Tribune';

export class PageTribuneSettings extends React.Component {
  static navigationOptions = ({navigation, screenProps}) => ({
    title: screenProps.tribune.configuration.title + ' settings',
  })

  render() {
    const configuration = this.props.screenProps.tribune.configuration

    return (
      <SectionList style={{flex: 1, backgroundColor: 'white'}}
        renderItem={({item}) => <ListItem navigation={this.props.navigation} title={item.title} settingKey={item.key} value={item.value} configuration={configuration} />}
        renderSectionHeader={({section}) => <ListHeader title={section.title} key={section.key} />}
        sections={[
          {data: [
            {title: 'Display name', key: "user_agent", value: configuration.user_agent},
            {title: 'Color', key: "color", value: configuration.color},
            {title: 'Backend', key: "backend", value: configuration.backend},
            {title: 'Post URL', key: "post_url", value: configuration.post_url},
            {title: 'Post format', key: "post_format", value: configuration.post_format},
            {title: 'Cookie', key: "cookie", value: configuration.cookie},
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

    this.currentText = this.props.value
  }

  onPress = () => {
    this.initialValue = this.state.value
    this.setState({modalVisible: true})
  }

  changeText = (text) => {
    this.currentText = text;
    this.setState({value: text})
  }

  changeValue = () => {
    this.setState({value: this.currentText})
    this.setState({modalVisible: false})

    // This is extremely ugly, but it's the only way I've found to
    // notify the postsView that it needs to re-render, from another
    // navigation page. It seems to work. The "page" param to the
    // navigation thingy is set in componentDidMount on the PageTribuneBrowser
    // class, and refers to this tribune's PageTribuneBrowser object.
    this.props.navigation.state.params.page.postsView.setState({color: this.currentText})

    this.props.configuration[this.props.settingKey] = this.currentText
    this.saveSettings()
  }

  saveSettings = () => {
    AsyncStorage
      .getItem("tribune:configuration")
      .then((result) => {
        if (result) {
          var globalConfiguration = JSON.parse(result)

          globalConfiguration.forEach((tribuneConfiguration) => {
            if (tribuneConfiguration.backend == this.props.configuration.backend) {
              tribuneConfiguration[this.props.settingKey] = this.state.value
            }
          })

          AsyncStorage.setItem('tribune:configuration', JSON.stringify(globalConfiguration))
        }
      })
  }

  render() {
    switch (this.props.settingKey) {
      case "color":
        return this.renderColorPicker()
      default:
        return this.renderTextInput()
    }
  }

  renderColorPicker() {
    const items = [
      "#fde096",
			"#67c6f2",
			"#f4b189",
			"#4bd0e3",
			"#eaa99e",
			"#7fe1cf",
			"#ddb1e5",
			"#c6d494",
			"#a7b7f2",
			"#e6c692",
			"#8bd0eb",
			"#dabf9f",
			"#80d8db",
			"#ebaec8",
			"#aad9a7",
			"#b6bee4",
			"#d0d9ae",
			"#94cabf",
			"#bdf5e9",
			"#a3daba"
		].map((color, i) => <Picker.Item key={i} label={"██ " + color} color={color} value={color} />)

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
              <Picker
                  selectedValue={this.state.value}
                  onValueChange={value => { this.changeText(value); }}
                  >
                  {items}
              </Picker>
              <Button title="OK" onPress={this.changeValue} />
            </View>
          </Modal>
          <Text style={styles.menuItemLabel}>{this.props.title}</Text>
          <Text style={styles.menuItemValue} numberOfLines={1} ellipsizeMode={'middle'}>{this.state.value}</Text>
        </View>
      </TouchableNativeFeedback>
    );
  }

  renderTextInput() {
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

