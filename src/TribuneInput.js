import React from 'react';
import { TextInput, View } from 'react-native';

import { styles } from './style';

export class TribuneInput extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      text: "",
    }
  }
  postMessage = () => {
    this.props.tribune.post(this.state.text)
  }

  onTextChange = (text) => {
    this.setState({text: text})
  }

  clear = () => {
    this.input.clear();
    this.setState({text: ""})
  }

  append = (text) => {
    this.setState({text: this.state.text + text})
    this.input.focus();
  }

  render() {
    return (
      <View style={styles.tribuneInput}>
        <TextInput ref={(ref) => { this.input = ref }}
            autoCapitalize={'sentences'}
            style={styles.tribuneInputText}
            onSubmitEditing={this.postMessage}
            placeholder='Your message'
            onChangeText={text => this.onTextChange(text)}
            value={this.state.text}
            blurOnSubmit={true}
            returnKeyType={'send'}
            />
      </View>
    );
  }
}

