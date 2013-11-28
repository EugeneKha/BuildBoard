/// <reference path='../_all.ts' />
module buildBoard {
    'use strict';

    export interface IBranchScope extends ng.IScope {
        branch:Branch;
        getPullRequestClass()
        getLastBuildStatus()
    }

    export class BranchLineController {
        public static NAME = 'branchLineController';


        public static $inject = [
            '$scope',
            '$http',
            '$window'
        ];

        constructor(public $scope:IBranchScope, $http:ng.IHttpService, $window:IBuildBoardWindow) {

            this.$scope.getPullRequestClass = ()=> {
                var pullRequest = this.$scope.branch.pullRequest;
                if (pullRequest && pullRequest.status) {
                    return pullRequest.status.isMerged ? 'btn-primary' : pullRequest.status.isMergeable ? 'btn-success' : 'btn-danger';
                }
                else {
                    return '';
                }
            };

            this.$scope.getLastBuildStatus = ()=> {
                if (this.$scope.branch.lastBuild) {
                    return this.$scope.branch.lastBuild.status;
                }
                else {
                    return '';
                }
            };


            if (this.$scope.branch.pullRequest && !this.$scope.branch.pullRequest.status) {
                $http.get($window.jsRoutes.controllers.Github.pullRequestStatus($scope.branch.pullRequest.id).absoluteURL()).success((data:PRStatus)=> {
                    this.$scope.branch.pullRequest.status = data;
                });
            }
            if (!this.$scope.branch.lastBuild) {
                $http.get($window.jsRoutes.controllers.Jenkins.lastBuildInfo($scope.branch.name).absoluteURL()).success((build:Build)=> {
                    this.$scope.branch.lastBuild = build;
                });
            }
        }

    }
}