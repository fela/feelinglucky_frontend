/* Controllers */

angular.module('myApp.controllers', [])

.controller('IndexCtrl', ['$scope', 'WS', function($scope, WS) {
	console.log("controller helloWorld", WS);
	$scope.transactionLog = WS.transactionLog;

  $scope.lotteryAmounts = [{amount: 1}, {amount: 2}, {amount: 3}];
  $scope.lotteryAmount = $scope.lotteryAmounts[1];

	$scope.playLottery = function() {
    var msg = {msgType: "playLottery", amount: $scope.lotteryAmount.amount};
    WS.sendMsg(msg);

    $('#myModal').modal('hide');
	};
}]);
