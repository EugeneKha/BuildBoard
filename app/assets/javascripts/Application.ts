/// <reference path='_all.ts' />


module buildBoard {
    'use strict';

    angular.module('buildBoard', ['ui.router', 'ui.bootstrap'])
        .service(BackendService.NAME, BackendService)
        .service(BranchesService.NAME, BranchesService)
        .service(LoggedUserService.NAME, LoggedUserService)
        .controller('branchesController', BranchesController)
        .controller(BranchLineController.NAME, BranchLineController)
        .controller(HeaderController.NAME, HeaderController)
        .controller(ArtifactsController.NAME, ArtifactsController)
        .controller(TestCaseController.NAME, TestCaseController)
        .filter('status', status)
        .filter('activeFilter', activeFilter)
        .filter('encode', encode)
        .filter('pullRequestStatus', pullRequestStatus)
        .directive('entityTitle', ()=>new EntityTitleDirective())
        .directive(EntityStateDirective.NAME, ()=>new EntityStateDirective())
        .directive(PanelDirective.NAME, ()=>new PanelDirective())
        .directive(PanelHeadingDirective.NAME, ()=>new PanelHeadingDirective())
        .directive(PanelBodyDirective.NAME, ()=>new PanelBodyDirective())
        .directive(BuildStatusDirective.NAME, ()=>new BuildStatusDirective())
        .config(($stateProvider:ng.ui.IStateProvider, $urlRouterProvider:ng.ui.IUrlRouterProvider)=> {

            $urlRouterProvider.otherwise("/list?user=my&branch=all");

            $stateProvider.state('list', {
                url: "/list?user&branch",
                templateUrl: "/assets/partials/main.html",
                controller: BranchesController
            })
                .state('list.branch', {
                    url: "/branch?name",
                    templateUrl: "/assets/partials/branch.html",
                    controller: BranchController
                })
                .state('list.branch.artifacts', {
                    params: ["name", "user", "branch", "build", "buildNode"],
                    templateUrl: "/assets/partials/artifacts.html",
                    controller: ArtifactsController
                })
                .state('list.branch.artifacts.testCase', {
                    params: ["name", "user", "branch", "build", "buildNode", "testCase"],
                    templateUrl: "/assets/partials/testCase.html",
                    controller: TestCaseController
                })

        }).run(function ($rootScope, $state) {
            $rootScope.$state = $state;
        })

}