// vim: et ts=2 sts=2 sw=2

export class Post {
  constructor(fields) {
    this.id = fields.id
    this.time = fields.time
    this.info = fields.info
    this.login = fields.login
    this.message = fields.message
    this.tribune = fields.tribune
  }

  export() {
    return {id: this.id, time: this.time, info: this.info, login: this.login, message: this.message};
  }

  clock() {
    if (this.time == undefined) {
      console.log(this);
      return;
    }
    var year = this.time.substr(0, 4);
    var month = this.time.substr(4, 2);
    var day = this.time.substr(6, 2);
    var hour = this.time.substr(8, 2);
    var minute = this.time.substr(10, 2);
    var second = this.time.substr(12, 2);

    return hour + ":" + minute + ":" + second;
  }

  author() {
    return this.login ? this.login : this.info;
  }
}

