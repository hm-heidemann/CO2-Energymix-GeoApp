var addCountryMode = false;
var countryAdded = false;
var chartAdded = false;

var minYear = 1750;
var maxYear = 2021;

var maxCo2 = 0;
var maxCo2PerCap = 0;

let selectedYear = maxYear;
var maxCo2Allowed = 0;
var numb_classes = 10;
var classwidth_between_fac = 1/numb_classes;
var grades_arr_co2 = [0, 20e6, 50e6, 100e6, 200e6, 500e6, 1e9, 2e9, 5e9, 10e9, Number.MAX_SAFE_INTEGER];
var grades_arr_co2_pc = [0, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, Number.MAX_SAFE_INTEGER];
colorRamp = chroma.scale('OrRd').classes(grades_arr_co2.length);
colorRampPerCapita = chroma.scale('OrRd').classes(grades_arr_co2_pc.length);

var addCountryBtn = document.getElementById('addCountryBtn');
addCountryBtn.style.display = countryAdded ? "block" : "none";

var chartButton = document.getElementById('clearChartBtn');
chartButton.style.display = chartAdded ? "block" : "none";

var yearSlider = document.getElementById('yearSlider');
var minYearDisplay = document.getElementById('minYear');
var maxYearDisplay = document.getElementById('maxYear');

yearSlider.min = minYear;
yearSlider.max = maxYear;
yearSlider.value = maxYear;
minYearDisplay.textContent = minYear;
maxYearDisplay.textContent = maxYear;

var co2EmissionsLayer = new L.GeoJSON(null, {onEachFeature: onEachFeature});
var co2EmissionsPerCapitaLayer = new L.GeoJSON(null, {onEachFeature: onEachFeaturePerCap});

googleSat = L.tileLayer('http://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
    maxZoom: 20,
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    attribution: "<a href=\'http://maps.google.com/\'>Google</a> Maps Satellite",
    noWrap: true
});

googleStreets = L.tileLayer('http://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
    maxZoom: 20,
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    attribution: "<a href=\'http://maps.google.com/\'>Google</a> Maps",
    noWrap: true
});


osmMapnik = new L.tileLayer(
        'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
        {
            maxZoom: 20,
            attribution: 'Map data © OpenStreetMap contributors'
        }
);

var map = L.map('mapid', {
    center: [30, 0],
    zoom: 2,
    layers: [osmMapnik, googleStreets, osmMapnik]
});

google.charts.load('current', {'packages':['corechart']});

function updateYearDisplay() {
    if (map.hasLayer(co2EmissionsLayer)) {
        document.getElementById('cardHeader').textContent = "CO2 Emissionen im Jahr " + selectedYear;
    } else if (map.hasLayer(co2EmissionsPerCapitaLayer)) {
        document.getElementById('cardHeader').textContent = "CO2 Emissionen pro Kopf im Jahr " + selectedYear;
    }
}

yearSlider.addEventListener('input', function(e) {
    selectedYear = parseInt(e.target.value);
    
    updateYearDisplay();

    if (map.hasLayer(co2EmissionsLayer)) {
        co2EmissionsLayer.setStyle(totalCo2Style);
        co2EmissionsLayer.eachLayer(function (layer) {
            onEachFeature(layer.feature, layer);
        });
    } else if (map.hasLayer(co2EmissionsPerCapitaLayer)) {
        co2EmissionsPerCapitaLayer.setStyle(totalCo2StylePerCapita);
        co2EmissionsPerCapitaLayer.eachLayer(function (layer) {
            onEachFeaturePerCap(layer.feature, layer);
        });
    }
});

document.getElementById("addCountryBtn").addEventListener("click", function() {
        addCountryBtn.classList.add('button-active');
        addCountryMode = true;
});

$.ajax({
    url: 'http://10.152.57.134:8080/geoserver/heidemann/ows',
    data: {
        version: '1.0.0',
        request: 'GetFeature',
        service: 'WFS',
        typeName: 'heidemann:world_co2_emissions',
        maxFeatures: '2000',
        outputFormat: 'application/json'
    },
    dataType: 'json',
    jsonpCallback: 'getJson',
    success: handleCo2Json
    });

$.ajax({
    url: 'http://10.152.57.134:8080/geoserver/heidemann/ows',
    data: {
        version: '1.0.0',
        request: 'GetFeature',
        service: 'WFS',
        typeName: 'heidemann:world_co2_emissions_pc',
        maxFeatures: '2000',
        outputFormat: 'application/json'
    },
    dataType: 'json',
    jsonpCallback: 'getJson',
    success: handleJsonPerCapita
});

function handleCo2Json(data) {
    co2EmissionsLayer.addData(data);
    maxCo2 = 0;
    for (var feature of data.features) {
        if (feature.properties && feature.properties.mannual_co2 && feature.properties.minyear) {
            if (feature.properties.mannual_co2 > maxCo2) {
                maxCo2 = feature.properties.mannual_co2;
            }
        }
    }
    co2EmissionsLayer.setStyle(totalCo2Style);
}

function handleJsonPerCapita(data) {
    co2EmissionsPerCapitaLayer.addData(data);
    maxCo2 = 0;
    for (var feature of data.features) {
        if (feature.properties && feature.properties.mannual_co2_pc && feature.properties.minyear) {
            if (feature.properties.mannual_co2 > maxCo2) {
                maxCo2PerCap = feature.properties.mannual_co2;
            }
        }
    }
    co2EmissionsPerCapitaLayer.setStyle(totalCo2StylePerCapita);
}

function getCo2Color(co2Value) {
    for (var i = 0; i < grades_arr_co2.length; i++) {
        if (co2Value <= grades_arr_co2[i]) {
            return colorRamp(i / (grades_arr_co2.length - 1)).hex();
        }
    }
}

function totalCo2Style(feature) {
    if (feature.properties && feature.properties.year && feature.properties.annual_co2) {
        var year_arr = JSON.parse(feature.properties.year);
        var yearIndex = year_arr.indexOf(selectedYear);
        if (yearIndex > -1) {
            annual_co2_arr = JSON.parse(feature.properties.annual_co2);
            var co2DataForYear = annual_co2_arr[yearIndex];

            if (co2DataForYear !== null) {
                return {
                    fillColor: getCo2Color(co2DataForYear),
                    weight: 2,
                    opacity: 1,
                    color: 'white',
                    dashArray: '3',
                    fillOpacity: 0.7
                };
            }
        }
    }
    return {
        fillColor: "gray",
        weight: 2,
        opacity: 1,
        color: 'white',
        dashArray: '3',
        fillOpacity: 0.5
    };
}

function getCo2ColorPerCapita(co2Value) {
    for (var i = 0; i < grades_arr_co2_pc.length; i++) {
        if (co2Value <= grades_arr_co2_pc[i]) {
            return colorRampPerCapita(i / (grades_arr_co2_pc.length - 1)).hex();
        }
    }
}

function totalCo2StylePerCapita(feature) {
    if (feature.properties && feature.properties.year && feature.properties.annual_co2_pc) {
        var year_arr = JSON.parse(feature.properties.year);
        var yearIndex = year_arr.indexOf(selectedYear);
        if (yearIndex > -1) {
            annual_co2_arr = JSON.parse(feature.properties.annual_co2_pc);
            var co2DataForYear = annual_co2_arr[yearIndex];

            if (co2DataForYear !== null) {
                return {
                    fillColor: getCo2ColorPerCapita(co2DataForYear),
                    weight: 2,
                    opacity: 1,
                    color: 'white',
                    dashArray: '3',
                    fillOpacity: 0.7
                };
            }
        }
    }
    return {
        fillColor: "gray",
        weight: 2,
        opacity: 1,
        color: 'white',
        dashArray: '3',
        fillOpacity: 0.5
    };
}

var dataMatrix = [];

function addCountryToChart(feature, layer) {
    var year_arr = JSON.parse(feature.properties.year).map(Number);
    var countryName = feature.properties.name_de;

    var co2_arr;
    if (map.hasLayer(co2EmissionsLayer)) {
        co2_arr = JSON.parse(feature.properties.annual_co2);
    } else if (map.hasLayer(co2EmissionsPerCapitaLayer)) {
        co2_arr = JSON.parse(feature.properties.annual_co2_pc);
    }

    dataMatrix[0].push(countryName);

    var index = 0;
    for(var i = 1; i < dataMatrix.length; i++) {
        if (index < year_arr.length && dataMatrix[i][0] === year_arr[index]) {
            dataMatrix[i].push(co2_arr[index]);
            index++;
        } else {
            dataMatrix[i].push(null);
        }
    }
    drawChart(dataMatrix);
}


function drawChartForCountry(feature, layer) {
    var year_arr = JSON.parse(feature.properties.year).map(Number);
    var countryName = feature.properties.name_de;

    var co2_arr;
    if (map.hasLayer(co2EmissionsLayer)) {
        co2_arr = JSON.parse(feature.properties.annual_co2);
    } else if (map.hasLayer(co2EmissionsPerCapitaLayer)) {
        co2_arr = JSON.parse(feature.properties.annual_co2_pc);
    }

    dataMatrix = [];
    for (var i = minYear; i <= maxYear; i++) {
        dataMatrix.push([i, null]);
    }
    
    dataMatrix.unshift(['Jahr', countryName]);

    for(var i = 0; i < year_arr.length; i++) {
        dataMatrix[year_arr[i] - minYear + 1][1] = co2_arr[i];
    }

    drawChart(dataMatrix);
}

function onEachFeature(feature, layer) {
    if (feature.properties && feature.properties.year && feature.properties.annual_co2) {
        layer.on('click', function (e) {
            if (addCountryMode) {
                addCountryToChart(feature, layer);
                addCountryBtn.classList.remove('button-active');
                addCountryMode = false;
            } else {
                drawChartForCountry(feature, layer);
            }
        });

        var year_arr = JSON.parse(feature.properties.year);
        var annual_co2_arr = JSON.parse(feature.properties.annual_co2);

        var yearIndex = year_arr.indexOf(selectedYear);
        if (yearIndex > -1) {
            var co2DataForYear = annual_co2_arr[yearIndex];
            layer.bindPopup('<pre>CO2 Emissionen für ' + selectedYear + ': ' + co2DataForYear/1000000 + ' Mio. t </pre>');
        } else {
            layer.bindPopup('<pre>' + JSON.stringify(feature.properties, null, ' ').replace(/[\{\}"]/g, '') + '</pre>');
        }
    } else {
        layer.bindPopup('<pre>' + JSON.stringify(feature.properties, null, ' ').replace(/[\{\}"]/g, '') + '</pre>');
    }
}

function onEachFeaturePerCap(feature, layer) {
    if (feature.properties && feature.properties.year && feature.properties.annual_co2_pc) {
        layer.on('click', function (e) {
            if (addCountryMode) {
                addCountryToChart(feature, layer);
                addCountryBtn.classList.remove('button-active');
                addCountryMode = false;
            } else {
                drawChartForCountry(feature, layer);
            }
        });

        var year_arr = JSON.parse(feature.properties.year);
        var annual_co2_pc_arr = JSON.parse(feature.properties.annual_co2_pc);

        var yearIndex = year_arr.indexOf(selectedYear);
        if (yearIndex > -1) {
            var co2DataForYear = annual_co2_pc_arr[yearIndex];
            layer.bindPopup('<pre>CO2 Emissionen pro Kopf für ' + selectedYear + ': ' + co2DataForYear + ' t </pre>');
        } else {
            layer.bindPopup('<pre>' + JSON.stringify(feature.properties, null, ' ').replace(/[\{\}"]/g, '') + '</pre>');
        }
    } else {
        layer.bindPopup('<pre>' + JSON.stringify(feature.properties, null, ' ').replace(/[\{\}"]/g, '') + '</pre>');
    }
}

map.addLayer(co2EmissionsLayer);

var baseMaps = {
    "Google Satellit": googleSat,
    "Google Streets": googleStreets,
    "OSM Map": osmMapnik,     
};

var overlayMaps = {
    "CO2 Emissionen": co2EmissionsLayer, 
    "CO2 Emissionen Pro Kopf" : co2EmissionsPerCapitaLayer
};

L.control.layers(baseMaps, overlayMaps).addTo(map);

var legend = L.control({position: 'bottomright'});

legend.onAdd = function (map) {
    var div = L.DomUtil.create('div', 'info legend');
    return div;
};

legend.addTo(map);

function updateLegend() {
    var div = legend.getContainer();
    div.innerHTML = '';
    
    if (map.hasLayer(co2EmissionsLayer)) {
        div.innerHTML += '<i style="background:gray"></i> Keine Daten<br>';
        for (var i = 0; i < grades_arr_co2.length - 1; i++) {
            var from = grades_arr_co2[i];
            var to = grades_arr_co2[i + 1];

            var fromFormatted = from >= 1e9 ? (from / 1e9) + 'Mrd. t' : from >= 1e6 ? (from / 1e6) + 'Mio. t' : from;
            var toFormatted = to >= 1e9 ? (to / 1e9) + 'Mrd. t' : to >= 1e6 ? (to / 1e6) + 'Mio. t' : to;

            div.innerHTML +=
                '<i style="background:' + getCo2Color(from + 1) + '"></i> ' +
                fromFormatted + (i < grades_arr_co2.length - 2 ? ' &ndash; ' + toFormatted + '<br>' : '+');
        }
    } else if (map.hasLayer(co2EmissionsPerCapitaLayer)) {
        div.innerHTML += '<i style="background:gray"></i> Keine Daten<br>';
        for (var i = 0; i < grades_arr_co2_pc.length - 1; i++) {
            var from = grades_arr_co2_pc[i];
            var to = grades_arr_co2_pc[i + 1];

            div.innerHTML +=
                '<i style="background:' + getCo2ColorPerCapita(from + 1) + '"t></i> ' +
                from + (i < grades_arr_co2_pc.length - 2 ? 't &ndash; ' + to + 't <br>' : 't+');
        }
    }
}

function clearChart() {
    chartAdded = false;
    chartButton.style.display = "none";

    var chart = document.getElementById('chart');
    chart.style.display = "none";

    countryAdded = false;
    addCountryBtn.style.display = "none";
}

map.on('overlayadd', function(e) {
    clearChart();
    updateLegend();   
    updateYearDisplay();
});
map.on('overlayremove', function(e) {
    clearChart();
    updateLegend();
    updateYearDisplay();
});

updateLegend();

function drawChart(dataMatrix) {
    chartAdded = true;
    chartButton.style.display = "block";

    var chart = document.getElementById('chart');
    chart.style.display = "block";

    countryAdded = true;
    addCountryBtn.style.display = "block";

    var data = google.visualization.arrayToDataTable(dataMatrix);

    var options = {
        title: 'CO2 Emissionen',
        curveType: 'function',
        legend: { position: 'bottom' },
        hAxis: {
            title: 'Jahr',
            format: '0'
        },
        vAxis: {
            title: 'Emissionen in Tonnen',
            viewWindow: { min: 0 },
            format: 'short'
        }
    };

    var chart = new google.visualization.LineChart(document.getElementById('chart'));

    var formatter = new google.visualization.NumberFormat({
        pattern: '#,##0.00',
        fractionDigits: 0
    });
    for (var i = 1; i < data.getNumberOfColumns(); i++) {
        formatter.format(data, i);
    }

    google.visualization.events.addListener(chart, 'ready', function () {
        var labels = chart.getContainer().getElementsByTagName('text');
        Array.prototype.forEach.call(labels, function(label) {
            if (/\d/.test(label.textContent)) {
                if (label.textContent.indexOf('K') > -1) {
                    label.textContent = label.textContent.replace('K', ' Tsd');
                }
                if (label.textContent.indexOf('M') > -1) {
                    label.textContent = label.textContent.replace('M', ' Mio');
                }
                if (label.textContent.indexOf('B') > -1) {
                    label.textContent = label.textContent.replace('B', ' Mrd');
                }
            }
        });
    });    

    chart.draw(data, options);
}