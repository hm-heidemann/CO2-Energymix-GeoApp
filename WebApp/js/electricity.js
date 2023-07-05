var currentLayerType = 'detailed';  

var chartAdded = false;
var pieChartsVisible = true;

var minYearEnergy = Number.MAX_SAFE_INTEGER;
var maxYearEnergy = 0;

var selectedYear = 2022;
var labels = ['Kohle', 'Gas', 'Nuklear', 'Hydro', 'Solar', 'Wind'];
var colors = {
    'Kohle': '#8B4513',
    'Gas': '#808080',
    'Nuklear': '#FF0000',
    'Hydro': '#0000FF',
    'Solar': '#FFFF00',
    'Wind': '#ADD8E6'
}; 
var simpleLabels = ['Erneuerbar', 'Fossil', 'Low Carbon'];
var simpleColors = {
    'Erneuerbar': '#4CAF50', 
    'Fossil': '#9E9E9E', 
    'Low Carbon': '#FFC107',
}

var addCountry = false;
var countryData = [['Energiequelle']];

var togglePieChartsButton = document.getElementById('togglePieCharts');
var addCountryBtn = document.getElementById('addCountryBtn');
var clearChartButton = document.getElementById('clearChartBtn');
var chart = document.getElementById('chart');

clearChartButton.style.display = "none";
addCountryBtn.style.display = "none";

function clearChart() {
    chartAdded = false;
    clearChartButton.style.display = "none";
    chart.style.display = "none";
    countryAdded = false;
    addCountryBtn.style.display = "none";
}

clearChartButton.addEventListener('click', clearChart);


togglePieChartsButton.addEventListener('click', function() {
    if (pieChartsVisible) {
        for (var i = 0; i < pieCharts.length; i++) {
            map.removeLayer(pieCharts[i]);
        }
        for (var i = 0; i < simplePieCharts.length; i++) {
            map.removeLayer(simplePieCharts[i]);
        }
        togglePieChartsButton.textContent = "Kreisdiagramme einblenden";
        togglePieChartsButton.classList.remove('button-active');
    } else {
        for (var i = 0; i < simplePieCharts.length; i++) {
            map.addLayer(simplePieCharts[i]);
        }
        for (var i = 0; i < pieCharts.length; i++) {
            map.addLayer(pieCharts[i]);
        }   
        togglePieChartsButton.textContent = "Kreisdiagramme ausblenden";
        togglePieChartsButton.classList.add('button-active');
    }
    pieChartsVisible = !pieChartsVisible;
});


google.charts.load('current', {'packages':['corechart']});

var electricityLayer = new L.GeoJSON(null, {
    style: function(feature) {
        return {
            fillColor: 'transparent',
            color: '#000',
            weight: 2,
            opacity: 1, 
        };
    },
    onEachFeature: onEachFeature
});

var simpleElectricityLayer = new L.GeoJSON(null, {
    style: function(feature) {
        return {
            fillColor: 'transparent',
            color: '#000',
            weight: 2,
            opacity: 1, 
        };
    },
    onEachFeature: onEachFeatureSimple
});

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

$.ajax({
    url: 'http://10.152.57.134:8080/geoserver/heidemann/ows',
    data: {
        version: '1.0.0',
        request: 'GetFeature',
        service: 'WFS',
        typeName: 'heidemann:world_electricity_share',
        maxFeatures: '2000',
        outputFormat: 'application/json'
    },
    dataType: 'json',
    jsonpCallback: 'getJson',
    success: handleEnergyJson
});

$.ajax({
    url: 'http://10.152.57.134:8080/geoserver/heidemann/ows',
    data: {
        version: '1.0.0',
        request: 'GetFeature',
        service: 'WFS',
        typeName: 'heidemann:world_electricity_share_simple',
        maxFeatures: '2000',
        outputFormat: 'application/json'
    },
    dataType: 'json',
    jsonpCallback: 'getJson',
    success: handleSimpleEnergyJson
});

function handleSimpleEnergyJson(data) {
    simpleElectricityLayer.addData(data);
}    

function handleEnergyJson(data) {
    electricityLayer.addData(data);
    for (var feature of data.features) {
        minyear_c = feature.properties.minyear_c;
        if (minyear_c !== null &&  minyear_c < minYearEnergy) {
            minYearEnergy = minyear_c;
        }
        if (feature.properties.maxyear_c > maxYearEnergy) {
            maxYearEnergy = feature.properties.maxyear_c;
        }
    }

    var yearPicker = document.getElementById('yearPicker');
    
    for (var i = minYearEnergy; i <= maxYearEnergy; i++) {
        var option = document.createElement('option');
        option.value = i;
        option.text = i;
        yearPicker.appendChild(option);
    }

    yearPicker.value = selectedYear;
}    

yearPicker.addEventListener('change', function(e) {
    selectedYear = parseInt(e.target.value);
    var title = "" 
    if (currentLayerType === 'detailed') title = "Elektrizitätsmix im Jahr " 
    else title = "Elektrizitätsmix (vereinfacht) im Jahr "
    document.getElementById('yearDisplay').textContent = title + selectedYear;

    togglePieChartsButton.textContent = "Kreisdiagramme ausblenden";
    togglePieChartsButton.classList.add('button-active');

    clearChart();
    removePieCharts();
    updatePieCharts();
});

function getDiameter(area) {
    var scaleFactor = 0.00002;
    var minDiameter = 35; 
    var maxDiameter = 50; 
    
    var diameter = Math.sqrt(area) * scaleFactor;
    diameter = Math.max(diameter, minDiameter);
    diameter = Math.min(diameter, maxDiameter);
    
    return diameter;
} 

var pieCharts = [];
var simplePieCharts = [];

function removePieCharts() {
    for (var i = 0; i < pieCharts.length; i++) {
        map.removeLayer(pieCharts[i]);
    }
    pieCharts = [];
}

function removeSimplePieCharts() {
    for (var i = 0; i < simplePieCharts.length; i++) {
        map.removeLayer(simplePieCharts[i]);
    }
    simplePieCharts = [];
}


function updatePieCharts() {
    togglePieChartsButton.textContent = "Kreisdiagramme ausblenden";
    togglePieChartsButton.classList.add('button-active');
    if (currentLayerType === 'simple') {
        removePieCharts();
    } else if (currentLayerType === 'detailed') {
        removeSimplePieCharts();
    }

    if (currentLayerType === 'simple') {
        simpleElectricityLayer.eachLayer(function (layer) {
            layer.off('click');
            onEachFeatureSimple(layer.feature, layer);
        });
    } else if (currentLayerType === 'detailed') {
        electricityLayer.eachLayer(function (layer) {
            layer.off('click');
            onEachFeature(layer.feature, layer);
        });
    }
}


document.getElementById('addCountryBtn').addEventListener('click', function() {
    addCountryBtn.classList.add('button-active');
    addCountry = true;
});

function drawChart() {
    var chart = document.getElementById('chart')
    chart.style.display = "block"; 
    clearChartButton.style.display = "block";
    addCountryBtn.style.display = "block";  
    
    var data = google.visualization.arrayToDataTable(countryData);

    var options = {
        title: 'Elektrizitätsmix im Jahr ' + selectedYear,
        legend: { position: 'bottom' },
        hAxis: {
            title: 'Prozentsatz',
            viewWindow: {
                min: 0,
                max: 100
            }
        },
        vAxis: {title: 'Energieträger', format: 'decimal'},
    };

    var chart = new google.visualization.BarChart(chart);

    chart.draw(data, options);
}


function onEachFeature(feature, layer) {
    var name = feature.properties.name_de;
    var center_lon = feature.properties.center_lon;
    var center_lat = feature.properties.center_lat;

    var year_c = feature.properties.year_c ? JSON.parse(feature.properties.year_c) : null;
    var year_g = feature.properties.year_g ? JSON.parse(feature.properties.year_g) : null;
    var year_n = feature.properties.year_n ? JSON.parse(feature.properties.year_n) : null;
    var year_h = feature.properties.year_h ? JSON.parse(feature.properties.year_h) : null;
    var year_s = feature.properties.year_s ? JSON.parse(feature.properties.year_s) : null;
    var year_w = feature.properties.year_w ? JSON.parse(feature.properties.year_w) : null;

    var coal = feature.properties.coal ? JSON.parse(feature.properties.coal) : null;
    var gas = feature.properties.gas ? JSON.parse(feature.properties.gas) : null;
    var nuclear = feature.properties.nuclear ? JSON.parse(feature.properties.nuclear) : null;
    var hydro = feature.properties.hydro ? JSON.parse(feature.properties.hydro) : null;
    var solar = feature.properties.solar ? JSON.parse(feature.properties.solar) : null;
    var wind = feature.properties.wind ? JSON.parse(feature.properties.wind) : null;

    var coalIndex = year_c ? year_c.indexOf(selectedYear) : -1;
    var gasIndex = year_g ? year_g.indexOf(selectedYear) : -1;
    var nuclearIndex = year_n ? year_n.indexOf(selectedYear) : -1;
    var hydroIndex = year_h ? year_h.indexOf(selectedYear) : -1;
    var solarIndex = year_s ? year_s.indexOf(selectedYear) : -1;
    var windIndex = year_w ? year_w.indexOf(selectedYear) : -1;

    var data = [];
    var labels = [];
    
    var bgColors = [];

    let energyTypes = [
        {name: 'Kohle', data: coal, index: coalIndex},
        {name: 'Gas', data: gas, index: gasIndex},
        {name: 'Nuklear', data: nuclear, index: nuclearIndex},
        {name: 'Hydro', data: hydro, index: hydroIndex},
        {name: 'Solar', data: solar, index: solarIndex},
        {name: 'Wind', data: wind, index: windIndex}
    ];

    for (let energy of energyTypes) {
        if (energy.index >= 0 && energy.data[energy.index]) {
            data.push(energy.data[energy.index]);
        } else {
            data.push(0);
        }
        labels.push(energy.name);
        bgColors.push(colors[energy.name]);
    }

    if (data.length > 0) {    
        var area = turf.area(layer.feature);
        var diameter = getDiameter(area);
        var pieOptions = {
            type: 'pie',
            width: diameter, height: diameter,
            data: data.map(val => parseFloat(val.toFixed(2))),
            colors: bgColors
        };   
              
        var center = {'lat': center_lat, 'lon': center_lon};
        var pieChart = L.minichart(center, pieOptions);
        pieCharts.push(pieChart);
        map.addLayer(pieChart); 
    }

    layer.on('click', function() {
        if (data.length > 0) {
            if (addCountry) {
                countryData[0].push(name);
                for (let i = 0; i < labels.length; i++) {
                    if (countryData[i + 1]) {
                        countryData[i + 1].push(data[i]);
                    } else {
                        countryData.push([labels[i], data[i]]);
                    }
                }
                addCountryBtn.classList.remove('button-active');
                addCountry = false;
            } else {
                countryData = [['Energiequelle', name]];
                for (let i = 0; i < labels.length; i++) {
                    countryData.push([labels[i], data[i]]);
                }
            }
    
            drawChart();
        }
    });    

    var popupContent = `
        <strong>${name}</strong><br/>
        ${labels.map((label, index) => `${label}: ${parseFloat(data[index].toFixed(2))}%`).join('<br/>')}
    `;
    layer.bindPopup(popupContent);
}

map.addLayer(electricityLayer);

function onEachFeatureSimple(feature, layer) {
    var name = feature.properties.name_de;
    var center_lon = feature.properties.center_lon;
    var center_lat = feature.properties.center_lat;

    var year_f = feature.properties.year_f ? JSON.parse(feature.properties.year_f) : null;
    var year_r = feature.properties.year_r ? JSON.parse(feature.properties.year_r) : null;
    var year_l = feature.properties.year_l ? JSON.parse(feature.properties.year_l) : null;

    var fossil = feature.properties.fossil ? JSON.parse(feature.properties.fossil) : null;
    var renewables = feature.properties.renewables ? JSON.parse(feature.properties.renewables) : null;
    var lowcarb = feature.properties.lowcarb ? JSON.parse(feature.properties.lowcarb) : null;

    var fossilIndex = year_f ? year_f.indexOf(selectedYear) : -1;
    var renewablesIndex = year_r ? year_r.indexOf(selectedYear) : -1;
    var lowcarbIndex = year_l ? year_l.indexOf(selectedYear) : -1;

    var pieData = [];
    var barData = [];
    var pieLabels = [];
    var barLabels = [];
    var bgColors = [];

    let energyTypes = [
        {name: 'Erneuerbar', data: renewables, index: renewablesIndex},
        {name: 'Fossil', data: fossil, index: fossilIndex}, 
        {name: 'Low Carbon', data: lowcarb, index: lowcarbIndex}
    ];
    
    let dataCount = 0;
    
    for (let energy of energyTypes) {
        if (energy.index >= 0 && energy.data[energy.index]) {
            barData.push(energy.data[energy.index]);
            barLabels.push(energy.name);
    
            if (energy.name !== 'Low Carbon') {
                pieData.push(energy.data[energy.index]);
                pieLabels.push(energy.name);
                bgColors.push(simpleColors[energy.name]);
            }
            dataCount++;
        } else {
            barData.push(0);
            if (energy.name !== 'Low Carbon') {
                pieData.push(0);
            }
        }
    }
    
    if (dataCount === 3) {
        var area = turf.area(layer.feature);
        var diameter = getDiameter(area);
        var pieOptions = {
            type: 'pie',
            width: diameter, height: diameter,
            data: pieData.map(val => parseFloat(val.toFixed(2))),
            colors: bgColors
        };   
                
        var center = {'lat': center_lat, 'lon': center_lon};
        var pieChart = L.minichart(center, pieOptions);
        simplePieCharts.push(pieChart);
        map.addLayer(pieChart); 
    }    

    layer.on('click', function() {
        if (barData.length > 0) {
            if (addCountry) {
                countryData[0].push(name);
                for (let i = 0; i < simpleLabels.length; i++) {
                    if (countryData[i + 1]) {
                        countryData[i + 1].push(barData[i]);
                    } else {
                        countryData.push([barLabels[i], barData[i]]);
                    }
                }
                addCountryBtn.classList.remove('button-active');
                addCountry = false;
            } else {
                countryData = [['Energiequelle', name]];
                for (let i = 0; i < barLabels.length; i++) {
                    countryData.push([barLabels[i], barData[i]]);
                }
            }
    
            drawChart();
        }
    });

    var popupContent = `
        <strong>${name}</strong><br/>
        ${barLabels.map((label, index) => `${label}: ${parseFloat(barData[index].toFixed(2))}%`).join('<br/>')}
    `;
    layer.bindPopup(popupContent);
}

var baseMaps = {
    "Google Satellit": googleSat,
    "Google Streets": googleStreets,
    "OSM Map": osmMapnik,     
};

var overlayMaps = {
    "Energiemix": electricityLayer, 
    "Energiemix vereinfacht": simpleElectricityLayer
};

L.control.layers(baseMaps, overlayMaps).addTo(map);

var legend = L.control({position: 'bottomright'});

legend.onAdd = function (map) {
    var div = L.DomUtil.create('div', 'info legend');
    return div;
};

legend.addTo(map);

L.control.scale({
    maxWidth: 100,
    metric: true, 
    imperial: false,
    updateWhenIdle: false 
}).addTo(map);

function getSimpleLegend() {
    var result = '';
    var simpleLabels = ['Erneuerbar', 'Fossil'];
    simpleLabels.forEach(label => {
        result += '<i style="background:' + simpleColors[label] + '"></i> ' +
            label + '<br>';
    });
    return result;
}

function getDetailedLegend() {
    var result = '';
    labels.forEach((label) => {
        result += 
        '<i style="background:' + colors[label] + '"></i> ' +
        (label ? label + '<br>' : '+');
    });
    return result;
}

function updateLegend() {
    var div = legend.getContainer();
    div.innerHTML = '';

    if (map.hasLayer(electricityLayer)) {
        div.innerHTML = getDetailedLegend();
    } else if (map.hasLayer(simpleElectricityLayer)) {
        div.innerHTML = getSimpleLegend();
    }
}


function updateCardTitle() {
    var title = "" 
    if (currentLayerType == 'detailed') title = "Energiemix im Jahr " 
    else title = "Energiemix (vereinfacht) im Jahr "
    document.getElementById('yearDisplay').textContent = title + selectedYear;
}

map.on('overlayadd', function(e) {
    if (map.hasLayer(electricityLayer)) {
        currentLayerType = 'detailed';
    } else if (map.hasLayer(simpleElectricityLayer)) {
        currentLayerType = 'simple';
    }
    clearChart();
    updatePieCharts();
    updateCardTitle();
    updateLegend();
});
map.on('overlayremove', function(e) {
    if (map.hasLayer(electricityLayer)) {
        currentLayerType = 'detailed';
    } else if (map.hasLayer(simpleElectricityLayer)) {
        currentLayerType = 'simple';
    }
    clearChart();
    updatePieCharts();
    updateCardTitle();
    updateLegend();
});

updateLegend();

