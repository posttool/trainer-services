var urlParams;
(window.onpopstate = function () {
    var match,
        pl     = /\+/g,
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
        query  = window.location.search.substring(1);

    urlParams = {};
    while (match = search.exec(query))
       urlParams[decode(match[1])] = decode(match[2]);
})();

var wavesurfer = Object.create(WaveSurfer);

$(document).ready(function () {

    wavesurfer.init({
        container: '#waveform',
        height: 200,
        minPxPerSec: 500,
        scrollParent: true,
        normalize: true,
        minimap: true,
//        backend: 'AudioElement'
    });

    wavesurfer.load('/wav?vid='+urlParams['vid']+'&uid='+urlParams['uid']);

    wavesurfer.enableDragSelection({
        color: color(200,255,240)
    });

    wavesurfer.on('ready', function () {
        addRegions();
    });

    var pregion;
    wavesurfer.on('region-click', function (region, e) {
        e.stopPropagation();
        if (pregion)
            pregion.update({color: color(200,200,200,.1)})
        region.update({color: color(200,200,200,.6)});
        pregion = region;
        e.shiftKey ? region.playLoop() : region.play();
    });

    wavesurfer.on('region-click', editAnnotation);
//    wavesurfer.on('region-updated', saveRegions);
//    wavesurfer.on('region-removed', saveRegions);
    wavesurfer.on('region-in', showNote);

    wavesurfer.on('region-play', function (region) {
//        region.once('out', function () {
//            wavesurfer.play(region.start);
//            wavesurfer.pause();
//        });
    });


    wavesurfer.initMinimap({
        height: 30,
        waveColor: '#ddd',
        progressColor: '#999',
        cursorColor: '#999'
    });


    wavesurfer.on('ready', function () {
        var timeline = Object.create(WaveSurfer.Timeline);
        timeline.init({
            wavesurfer: wavesurfer,
            container: "#wave-timeline"
        });
    });


    var playButton = $('#play');
    var pauseButton = $('#pause');
    wavesurfer.on('play', function () {
        playButton.hide()
        pauseButton.show();
    });
    wavesurfer.on('pause', function () {
        playButton.show()
        pauseButton.hide();
    });
});





function color(r,g,b,alpha) {
    return 'rgba(' + [ r, g, b, alpha || 1 ] + ')';
}


/**
 * Edit annotation for a region.
 */
function editAnnotation (region) {
    var form = document.forms.edit;
    form.style.opacity = 1;
    form.elements.start.value = Math.round(region.start * 10) / 10,
    form.elements.end.value = Math.round(region.end * 10) / 10;
    form.elements.note.value = JSON.stringify(region.data);
    form.onsubmit = function (e) {
        e.preventDefault();
        region.update({
            start: form.elements.start.value,
            end: form.elements.end.value,
            data: {
                note: form.elements.note.value
            }
        });
        form.style.opacity = 0;
    };
    form.onreset = function () {
        form.style.opacity = 0;
        form.dataset.region = null;
    };
    form.dataset.region = region.id;
}


function addRegions(){
    console.log(sm);
    sm.document.paragraphs.forEach(function(paragraph){
        paragraph.sentences.forEach(function(sentence){
            sentence.phrases.forEach(function(phrase){
                // boundary;
                phrase.words.forEach(function(word){
                    //depth, pos, text
                    if (word.syllables)
                        word.syllables.forEach(function(syllable){
                            syllable.forEach(function(ph){
                                wavesurfer.addRegion({
                                    start: ph.begin,
                                    end: ph.end,
                                    drag: false,
                                    color: color(200,200,200,.1),
                                    data: ph
                                });
                            })
                        });
                });
            })
        })
    });
}


function showNote (region) {
//    if (!showNote.el) {
//        showNote.el = document.querySelector('#subtitle');
//    }
//    showNote.el.textContent = region.data.note || '–';
}

//GLOBAL_ACTIONS['delete-region'] = function () {
//    var form = document.forms.edit;
//    var regionId = form.dataset.region;
//    if (regionId) {
//        wavesurfer.regions.list[regionId].remove();
//        form.reset();
//    }
//};
//
//GLOBAL_ACTIONS['export'] = function () {
//    window.open('data:application/json;charset=utf-8,' +
//        encodeURIComponent(localStorage.regions));
//};