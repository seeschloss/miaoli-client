// vim: et ts=2 sts=2 sw=2

import React from 'react';
import { TextInput, TouchableHighlight, View, Text, Keyboard } from 'react-native';

import { styles } from './style';

export class TribuneInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      text: "",
      display: "none",
      newPostButtonDisplay: "flex",
    }
  }

  postMessage = () => {
    this.props.tribune.post(this.state.text)
      .then(posts => {
        this.clear();
        Keyboard.dismiss();
      })
  }

  onTextChange = (text) => {
    this.setState({text: text, display: "flex"})
  }

  clear = () => {
    this.input.clear();
    this.setState({text: "", display: "none"})
  }

  append = (text) => {
    this.setState({text: this.state.text + text, display: "flex"})
    this.input.focus();
  }

  render() {
    return (
      <View style={[styles.tribuneInput]}>
        <TextInput ref={(ref) => { this.input = ref }}
            autoCapitalize={'sentences'}
            style={[styles.tribuneInputText, {display: this.state.display}]}
            onSubmitEditing={this.postMessage}
            placeholder='Your message'
            onChangeText={text => this.onTextChange(text)}
            value={this.state.text}
            blurOnSubmit={true}
            returnKeyType={'send'}
            />

        <PostButton ref={(ref) => { this.postButton = ref; }} input={this} />
      </View>
    );
  }
}

class PostButton extends React.Component {
  constructor(props) {
    super(props)
  }

  showInput = () => {
    this.props.input.append(' ')
    this.props.input.setState({newPostButtonDisplay: "none"})
  }

  render() {
    return (
      <TouchableHighlight style={[styles.newPostButtonWrapper, {display: this.props.input.state.newPostButtonDisplay}]} onPress={this.showInput}>
        <View style={styles.newPostButton}>
          <Text style={styles.newPostButtonText}>📧</Text>
        </View>
      </TouchableHighlight>
    );
  }
}

