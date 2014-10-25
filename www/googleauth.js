(function (cordova) {
  var GoogleAuth = function () {

  };

  GoogleAuth.prototype.chooseAccount = function (options, success, fail) {
    return cordova.exec(success, fail, 'GoogleAuth', 'chooseAccount', [options]);
  };

  GoogleAuth.prototype.getAuthToken = function (options, success, fail) {
    // XXX: Create one 'standard' callback (with result, err)?
    // XXX: Map err to exception?
    return cordova.exec(success, fail, 'GoogleAuth', 'getAuthToken', [options]);
  };

  window.googleAuth = new GoogleAuth();

  // backwards compatibility
  window.plugins = window.plugins || {};
  window.plugins.googleAuth = window.googleAuth;
})(window.PhoneGap || window.Cordova || window.cordova);