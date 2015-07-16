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
var pregion, nextregion = null;

$(document).ready(function () {

    wavesurfer.init({
        container: '#waveform',
        height: 200,
        minPxPerSec: 500,
        scrollParent: true,
        normalize: true,
        minimap: true
    });

    wavesurfer.load('/wav?vid='+urlParams['vid']+'&uid='+urlParams['uid']);

    wavesurfer.enableDragSelection({
        color: color(200,255,240)
    });

    wavesurfer.on('ready', function () {
        addRegionsWords();
    });

    wavesurfer.on('region-click', function (region, e) {
        e.stopPropagation();
        showNote(region,  e.shiftKey);
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

    $(document).keypress(function(e){
        if (e.keyCode == 46) {
            showNote(nextregion);
        }
    });

    $("#word").click(function(){
        pregion =null;
        addRegionsWords();
    })
    $("#syll").click(function(){
        pregion =null;
        addRegionsSyllables();
    })
    $("#ph").click(function(){
        pregion =null;
        addRegionsPhones();
    })

});





function color(r,g,b,alpha) {
    return 'rgba(' + [ r, g, b, alpha || 1 ] + ')';
}





function addRegionsPhones(){
    wavesurfer.clearRegions();
    sm.document.paragraphs.forEach(function(paragraph){
        paragraph.sentences.forEach(function(sentence){
            sentence.phrases.forEach(function(phrase){
                // boundary;
                phrase.words.forEach(function(word){
                    //depth, pos, text
                    if (word.syllables) {
                        word.syllables.forEach(function(syllable){
                            syllable.forEach(function(ph){
                                var r = {
                                    start: ph.begin,
                                    end: ph.end,
                                    drag: false,
                                    color: color(200,200,200,.1),
                                    data: ph
                                }
                                wavesurfer.addRegion(r);
                                if (nextregion == null) {
                                    for (var p in wavesurfer.regions.list)
                                        nextregion = wavesurfer.regions.list[p];
                                }
                            });
                        });
                    }
                });
            })
        })
    });
}

function addRegionsSyllables(){
    wavesurfer.clearRegions();
    sm.document.paragraphs.forEach(function(paragraph){
        paragraph.sentences.forEach(function(sentence){
            sentence.phrases.forEach(function(phrase){
                // boundary;
                phrase.words.forEach(function(word){
                    //depth, pos, text
                    if (word.syllables) {
                        word.syllables.forEach(function(syllable){
                            var r = {
                                start: syllable[0].begin,
                                end: syllable[syllable.length-1].end,
                                drag: false,
                                color: color(200,200,200,.1),
                                data: syllable
                                };
                            wavesurfer.addRegion(r);
                            if (nextregion == null) {
                                for (var p in wavesurfer.regions.list)
                                    nextregion = wavesurfer.regions.list[p];
                            }
                        });
                    }
                });
            })
        })
    });
}


function addRegionsWords(){
    wavesurfer.clearRegions();
    sm.document.paragraphs.forEach(function(paragraph){
        paragraph.sentences.forEach(function(sentence){
            sentence.phrases.forEach(function(phrase){
                // boundary;
                phrase.words.forEach(function(word){
                    if (word.syllables) {
                     var begin = 11111111, end  =0;
                        word.syllables.forEach(function(syllable){
                            begin = Math.min(begin, syllable[0].begin)
                            end = Math.max(end, syllable[syllable.length-1].end)
                     });
                       var r = {
                            start: begin,
                            end: end,
                            drag: false,
                            color: color(200,200,200,.1),
                            data: word
                            };
                        wavesurfer.addRegion(r);
                        if (nextregion == null) {
                            for (var p in wavesurfer.regions.list)
                                nextregion = wavesurfer.regions.list[p];
                        }
                    }
                });
            })
        })
    });
}



function showNote (region, loop) {
    var rsort = [];
    for (var p in wavesurfer.regions.list) {
        rsort.push(wavesurfer.regions.list[p])
    }
    rsort.sort(function(a,b){
        return a.end - b.end;
    });

    for (var i=0; i<rsort.length; i++) {
        var r = rsort[i];
        if(r.id == region.id) {
            nextregion = rsort[i+1];
        }
    }
    if (pregion)
        pregion.update({color: color(200,200,200,.1)})
    region.update({color: color(200,200,200,.6)});
    pregion = region;
    editAnnotation(region);
    if (loop)
        region.playLoop();
    else
       region.play();

}

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

function deleteRegion () {
    var form = document.forms.edit;
    var regionId = form.dataset.region;
    if (regionId) {
        wavesurfer.regions.list[regionId].remove();
        form.reset();
    }
};
