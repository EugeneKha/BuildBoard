/// <reference path='../_all.ts' />
module buildBoard {
    'use strict';

    export class BranchController extends BranchControllerBase {
        public static $inject = [
            '$scope',
            '$state',
            BackendService.NAME
        ];

        constructor($scope:IBranchScope, $state:ng.ui.IStateService, backendService:BackendService) {
            super($scope, backendService);


            this.$scope.branchName = $state.params['name'];
            this.$scope.closeView = ()=> {
                $state.go("list");
            };

            this.$scope.getArtifact = (node:BuildNode) => {
                if (node.artifactsUrl && !node.testCasePackage) {
                    backendService.getArtifact(node.artifactsUrl).success(testCasePackage => {
                        if (testCasePackage.length) {
                            node.testCasePackage = testCasePackage[0];
                        }
                    });
                }
            };

            backendService.branch(this.$scope.branchName).success(branch => {
                this.$scope.branch = branch;
                this.loadPullRequestStatus(this.$scope.branch);
            });

            backendService.builds(this.$scope.branchName).success(builds => {
                this.$scope.builds = builds;
                this.$scope.branch.lastBuild = _.first(builds)
            });
        }
    }
}