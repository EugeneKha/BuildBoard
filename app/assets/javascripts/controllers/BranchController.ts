/// <reference path='../_all.ts' />
module buildBoard {
    'use strict';

    export interface IBranchDetailsScope extends IBranchScope {
        loadBuild(buildInfo:BuildInfo): void;
        getActivity(): ActivityEntry[];
    }

    export class BranchController extends BranchControllerBase {
        public static $inject = [
            '$scope',
            '$state',
            BackendService.NAME,
            ModelProvider.NAME
        ];

        constructor(public $scope:IBranchDetailsScope, $state:ng.ui.IStateService, backendService:BackendService, modelProvider:ModelProvider) {
            super($scope, backendService, modelProvider);

            this.$scope.branchName = $state.params['name'];
            this.$scope.closeView = ()=> {
                $state.go("list");
            };

            var defer = null;
            this.$scope.loadBuild = (buildInfo:BuildInfo) => {
                if (buildInfo.node == null && !defer) {
                    defer = backendService.build(this.$scope.getBranch().name, buildInfo.number).success((build:BuildInfo) => {
                        buildInfo.node = build.node;
                        defer = null;
                    });
                }
            };

            this.$scope.getActivity = () => {
                var branch = this.$scope.getBranch();
                if (!branch)
                    return null;

                return branch.activity;
            }
        }
    }
}