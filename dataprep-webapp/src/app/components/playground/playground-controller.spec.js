/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Playground controller', function () {
    'use strict';

    var createController, scope, stateMock;
    var datasets = [
        {
            'id': 'de3cc32a-b624-484e-b8e7-dab9061a009c',
            'name': 'customers_jso_light',
            'author': 'anonymousUser',
            'records': 15,
            'nbLinesHeader': 1,
            'nbLinesFooter': 0,
            'created': '03-30-2015 08:06'
        },
        {
            'id': '3b21388c-f54a-4334-9bef-748912d0806f',
            'name': 'customers_jso',
            'author': 'anonymousUser',
            'records': 1000,
            'nbLinesHeader': 1,
            'nbLinesFooter': 0,
            'created': '03-30-2015 07:35'
        }
    ];

    var preparations = [
        {
            'id': 'ab136cbf0923a7f11bea713adb74ecf919e05cfa',
            'dataSetId': 'de3cc32a-b624-484e-b8e7-dab9061a009c',
            'author': 'anonymousUser',
            'creationDate': 1427447300300
        },
        {
            'id': 'fbaa18e82e913e97e5f0e9d40f04413412be1126',
            'dataSetId': '3b21388c-f54a-4334-9bef-748912d0806f',
            'author': 'anonymousUser',
            'creationDate': 1427447330693
        }
    ];
    beforeEach(angular.mock.module('data-prep.playground', function ($provide) {
        stateMock = {playground: {
            dataset: {},
            lookup: {
                actions: []
            },
            previousState: 'nav.home.preparations',
            preparationName: ''
        }};
        $provide.constant('state', stateMock);
    }));

    beforeEach(inject(function ($rootScope, $q, $controller, $state, PlaygroundService) {
        scope = $rootScope.$new();

        createController = function () {
            return $controller('PlaygroundCtrl', {
                $scope: scope
            });
        };

        spyOn(PlaygroundService, 'createOrUpdatePreparation').and.returnValue($q.when(true));
        spyOn($state, 'go').and.returnValue();

    }));

    describe('bindings', function () {
        it('should bind previewInProgress getter with PreviewService', inject(function (PreviewService) {
            //given
            var ctrl = createController();
            expect(ctrl.previewInProgress).toBeFalsy();

            //when
            spyOn(PreviewService, 'previewInProgress').and.returnValue(true);

            //then
            expect(ctrl.previewInProgress).toBe(true);
        }));
    });

    describe('recipe header', function () {

        it('should create/update preparation with clean name on name edition confirmation', inject(function (PlaygroundService) {
            //given
            var ctrl = createController();
            stateMock.playground.preparationName = '  my new name  ';

            //when
            ctrl.confirmPrepNameEdition(stateMock.playground.preparationName);

            //then
            expect(PlaygroundService.createOrUpdatePreparation).toHaveBeenCalledWith('my new name');
        }));

        it('should change route to preparation route on name edition confirmation', inject(function ($rootScope, $state) {
            //given
            var ctrl = createController();
            stateMock.playground.preparationName = '  my new name  ';
            stateMock.playground.preparation = {id: 'fe6843da512545e'};

            //when
            ctrl.confirmPrepNameEdition(stateMock.playground.preparationName);
            $rootScope.$digest();

            //then
            expect($state.go).toHaveBeenCalledWith('nav.home.preparations', {}, {
                location: 'replace',
                inherit: false
            });
        }));

        it('should not call service create/updateName service if name is blank on name edition confirmation', inject(function (PlaygroundService) {
            //given
            var ctrl = createController();

            //when
            ctrl.confirmPrepNameEdition(' ');

            //then
            expect(PlaygroundService.createOrUpdatePreparation).not.toHaveBeenCalled();
        }));
    });

    describe('implicit preparation', function () {
        var ctrl;
        var preparation;

        beforeEach(inject(function ($q, PreparationService, StateService) {
            preparation = {id: '9af874865e42b546', draft: true};
            stateMock.playground.preparation = preparation;
            stateMock.playground.previousState = 'nav.home.preparations';

            spyOn(PreparationService, 'delete').and.returnValue($q.when(true));
            spyOn(StateService, 'resetPlayground').and.returnValue();

            ctrl = createController();

        }));

        it('should return true (allow playground close) with NOT implicit preparation', function () {
            //given
            preparation.draft = false;
            spyOn(ctrl, 'close').and.returnValue();

            //when
            ctrl.beforeClose();

            //then
            expect(ctrl.close).toHaveBeenCalled();
        });

        it('should return false (block playground close) with implicit preparation', function () {
            //when
            ctrl.beforeClose();

            //then
            expect(ctrl.showNameValidation).toBe(true);
        });

        it('should show save/discard modal with implicit preparation', function () {
            //given
            expect(ctrl.showNameValidation).toBeFalsy();

            //when
            ctrl.beforeClose();

            //then
            expect(ctrl.showNameValidation).toBe(true);
        });

        it('should delete current preparation on save discard', inject(function (PreparationService) {
            //when
            ctrl.discardSaveOnClose();

            //then
            expect(PreparationService.delete).toHaveBeenCalledWith(preparation);
        }));

        it('should go back to previous state on save discard', inject(function ($state, StateService) {
            //given
            ctrl.showNameValidation = true;

            //when
            ctrl.discardSaveOnClose();
            scope.$digest();

            //then
            expect(ctrl.showNameValidation).toBe(false);
            expect(StateService.resetPlayground).toHaveBeenCalled();
            expect($state.go).toHaveBeenCalledWith('nav.home.preparations');
        }));

        it('should change preparation name on save confirm', inject(function (PlaygroundService) {
            //given
            stateMock.playground.preparationName = '  my preparation ';

            //when
            ctrl.confirmSaveOnClose();

            //then
            expect(PlaygroundService.createOrUpdatePreparation).toHaveBeenCalledWith('my preparation');
        }));

        it('should hide save/discard and playground modals on save confirm', inject(function ($state, StateService) {
            //given
            ctrl.showNameValidation = true;

            //when
            ctrl.confirmSaveOnClose();
            scope.$digest();

            //then
            expect(ctrl.showNameValidation).toBe(false);
            expect(StateService.resetPlayground).toHaveBeenCalled();
            expect($state.go).toHaveBeenCalledWith('nav.home.preparations');
        }));
    });

    describe('lookup', function () {
        beforeEach(inject(function ($q, LookupService, StateService) {
            spyOn(LookupService, 'initLookups').and.returnValue($q.when());
            spyOn(StateService, 'setLookupVisibility').and.returnValue();
        }));

        it('should load lookup panel when it is hidden', inject(function (LookupService) {
            //given
            stateMock.playground.lookup.visibility = false;
            var ctrl = createController();

            //when
            ctrl.toggleLookup();

            //then
            expect(LookupService.initLookups).toHaveBeenCalled();
        }));

        it('should display lookup panel when it is hidden', inject(function (StateService) {
            //given
            stateMock.playground.lookup.visibility = false;
            var ctrl = createController();

            //when
            ctrl.toggleLookup();
            scope.$digest();

            //then
            expect(StateService.setLookupVisibility).toHaveBeenCalledWith(true, undefined);
        }));

        it('should hide lookup panel when it is visible', inject(function (LookupService, StateService) {
            //given
            stateMock.playground.lookup.visibility = true;
            var ctrl = createController();

            //when
            ctrl.toggleLookup();

            //then
            expect(LookupService.initLookups).not.toHaveBeenCalled();
            expect(StateService.setLookupVisibility).toHaveBeenCalledWith(false);
        }));
    });

    describe('feedback', function() {
        beforeEach(inject(function (StateService) {
            spyOn(StateService, 'showFeedback').and.returnValue();
        }));

        it('should open feedback modal', inject(function (StateService) {
            //given
            var ctrl = createController();

            //when
            ctrl.openFeedbackForm();

            //then
            expect(StateService.showFeedback).toHaveBeenCalled();
        }));
    });

    describe('dataset parameters', function() {
        beforeEach(inject(function ($q, StateService, PlaygroundService) {
            spyOn(StateService, 'hideDatasetParameters').and.returnValue();
            spyOn(StateService, 'toggleDatasetParameters').and.returnValue();
            spyOn(StateService, 'setIsSendingDatasetParameters').and.returnValue();
            spyOn(PlaygroundService, 'changeDatasetParameters').and.returnValue($q.when());
        }));

        it('should open dataset parameters', inject(function (StateService) {
            //given
            var ctrl = createController();
            expect(StateService.toggleDatasetParameters).not.toHaveBeenCalled();

            //when
            ctrl.toggleParameters();

            //then
            expect(StateService.toggleDatasetParameters).toHaveBeenCalled();
        }));

        it('should manage progress flag', inject(function (StateService) {
            //given
            var ctrl = createController();
            var parameters = {separator: ';', encoding: 'UTF-8'};

            expect(StateService.setIsSendingDatasetParameters).not.toHaveBeenCalled();

            //when
            ctrl.changeDatasetParameters(parameters);
            expect(StateService.setIsSendingDatasetParameters).toHaveBeenCalledWith(true);
            scope.$digest();

            //then
            expect(StateService.setIsSendingDatasetParameters).toHaveBeenCalledWith(false);
        }));

        it('should call parameter change function', inject(function (PlaygroundService) {
            //given
            var ctrl = createController();
            var parameters = {separator: ';', encoding: 'UTF-8'};

            expect(PlaygroundService.changeDatasetParameters).not.toHaveBeenCalled();

            //when
            ctrl.changeDatasetParameters(parameters);

            //then
            expect(PlaygroundService.changeDatasetParameters).toHaveBeenCalled();
        }));

        it('should hide dataset parameters', inject(function (StateService) {
            //given
            var ctrl = createController();
            var parameters = {separator: ';', encoding: 'UTF-8'};

            expect(StateService.hideDatasetParameters).not.toHaveBeenCalled();

            //when
            ctrl.changeDatasetParameters(parameters);
            scope.$digest();

            //then
            expect(StateService.hideDatasetParameters).toHaveBeenCalled();
        }));
    });

    describe('initialization', function() {
        beforeEach(inject(function (MessageService, PlaygroundService) {
            spyOn(MessageService, 'error').and.returnValue();
            spyOn(PlaygroundService, 'load').and.returnValue();
            spyOn(PlaygroundService, 'initPlayground').and.returnValue();
            stateMock.inventory= {preparations : preparations, datasets: datasets};
        }));

        it('should load playground', inject(function ($stateParams,PlaygroundService) {

            $stateParams.prepid = 'ab136cbf0923a7f11bea713adb74ecf919e05cfa';

            //given
            createController();

            //then
            expect(PlaygroundService.load).toHaveBeenCalledWith(preparations[0]);
        }));

        it('should go back to previous state from preparation playground', inject(function ($state, $stateParams,MessageService) {

            $stateParams.prepid = '1';

            //given
            createController();

            //then
            expect(MessageService.error).toHaveBeenCalled();
            expect($state.go).toHaveBeenCalledWith('nav.home.preparations');
        }));

        it('should init playground', inject(function ($stateParams,PlaygroundService) {

            $stateParams.prepid = '';
            $stateParams.datasetid = 'de3cc32a-b624-484e-b8e7-dab9061a009c';

            //given
            createController();

            //then
            expect(PlaygroundService.initPlayground).toHaveBeenCalledWith(datasets[0]);
        }));

        it('should go back to previous state from dataset playground', inject(function ($state, $stateParams,MessageService) {

            $stateParams.prepid = '';
            $stateParams.datasetid = '1';

            //given
            createController();

            //then
            expect(MessageService.error).toHaveBeenCalled();
            expect($state.go).toHaveBeenCalledWith('nav.home.preparations');
        }));
    });
});
