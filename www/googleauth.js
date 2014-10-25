(function (cordova) {
  var GoogleAuth = function () {

  };

  GoogleAuth.prototype.chooseAccount = function (options, success, fail) {
    return cordova.exec(function (args) {
      success(args);
    }, function (args) {
      fail(args);
    }, 'GoogleAuth', 'chooseAccount', [options]);
  };

  window.googleAuth = new GoogleAuth();

  // backwards compatibility
  window.plugins = window.plugins || {};
  window.plugins.googleAuth = window.googleAuth;
})(window.PhoneGap || window.Cordova || window.cordova);