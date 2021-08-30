/**
 * Build widget configuration
 */
(function () {
	'use strict';

	angular.module(HygieiaConfig.module).controller('RepoConfigController',
		RepoConfigController);

	RepoConfigController.$inject = ['modalData', '$uibModalInstance',
		'collectorData', '$scope'];
	function RepoConfigController(modalData, $uibModalInstance, collectorData, $scope) {
		var ctrl = this;
		var widgetConfig = modalData.widgetConfig;
		var component = modalData.dashboard.application.components[0];

		var savedRepositoryJob;

		// Request collectors
		ctrl.repoName = widgetConfig.options.repoName;
		ctrl.repoPersonalAccessToken = widgetConfig.options.personalAccessToken;

		// public variables
		ctrl.submitted = false;

		// public methods
		ctrl.submit = submit;


		ctrl.$onInit = function () {
			loadSavedRepositoryJob();
		};

		function loadSavedRepositoryJob() {
			var repositoryCollectorItems = component.collectorItems.SCM;
			
			savedRepositoryJob = repositoryCollectorItems ? repositoryCollectorItems[0].description : null;

			if (savedRepositoryJob) {
				$scope.getRepositoryCollectors(savedRepositoryJob).then(getRepositoryJobCollectorsCallback);
			}
		}

		$scope.getRepositoryCollectors = function (filter) {
			return collectorData.itemsByType('SCM', { "search": filter, "size": 20, "sort": "description" }).then(function (response) {
				return response;
			});
		};


		function getRepositoryJobCollectorsCallback(data) {
			for (let index = 0; index < data.length; index++) {
				const element = data[index];
				if (element.description == savedRepositoryJob) {
					ctrl.repositoryCollectorItem = element;
					break;
				}
			}
		}

		function submit(form) {
			ctrl.submitted = true;
			if (form.$valid) {
				var collectorItems = [ctrl.repositoryCollectorItem.id];
				var postObj = {
					name: 'repo',
					options: {
						id: widgetConfig.options.id,
						repoName: ctrl.repositoryCollectorItem.options.repositoryName,
						repoId: ctrl.repositoryCollectorItem.options.repositoryId,
						remoteUrl: ctrl.repositoryCollectorItem.options.remoteUrl,
						personalAccessToken: getNonNullString(ctrl.repoPersonalAccessToken)
					},
					componentId: component.id,
					collectorItemIds: collectorItems
				};
				$uibModalInstance.close(postObj);
			}
		}

		function getNonNullString(value) {
			return _.isEmpty(value) || _.isUndefined(value) ? "" : value
		}
	}
})();
