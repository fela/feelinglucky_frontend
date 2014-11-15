/* Controllers */

angular.module('myApp.controllers', [])

.controller('IndexCtrl', ['$scope', '$cookies', '$cookieStore', 'WS', function($scope, $cookies, $cookieStore, WS) {
  console.log("controller helloWorld", $cookies.accName);
  $scope.transactionLog = WS.transactionLog;

  $scope.lotteryAmounts = [{amount: 1}, {amount: 2}, {amount: 3}];
  $scope.lotteryAmount = $scope.lotteryAmounts[1];
  $scope.accountName = $cookies.accName;//$cookies.accName.substring(1, $cookies.accName.length-1);

  $scope.playLottery = function() {
  var msg = {
      msgType: "playLottery", 
      amount: $scope.lotteryAmount.amount,
      accName: $cookies.accName
  };
  WS.sendMsg(msg);

  $('#myModal').modal('hide');
  };
}]);
