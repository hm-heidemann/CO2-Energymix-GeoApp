var minYearEnergy = Number.MAX_SAFE_INTEGER;
var maxYearEnergy = 0;

var maxCo2 = 0;

var selectedYear = 2021;
var labels = ['Kohle', 'Gas', 'Öl', 'Nuklear', 'Hydro', 'Solar', 'Wind'];
var colors = {
    'Kohle': '#8B4513',
    'Gas': '#808080',
    'Öl': '#000000',
    'Nuklear': '#FF0000',
    'Hydro': '#0000FF',
    'Solar': '#FFFF00',
    'Wind': '#ADD8E6'
}; 

var addCountry = false;
var countryData = [['Energiequelle']];

google.charts.load('current', {'packages':['corechart']});

var energyLayer = new L.GeoJSON(null, {
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
        typeName: 'heidemann:world_energy_share',
        maxFeatures: '2000',
        outputFormat: 'application/json'
    },
    dataType: 'json',
    jsonpCallback: 'getJson',
    success: handleEnergyJson
});

function handleEnergyJson(data) {
    energyLayer.addData(data);
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

    removePieCharts();

    energyLayer.eachLayer(function (layer) {
        onEachFeature(layer.feature, layer);
    });
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

function removePieCharts() {
    for (var i = 0; i < pieCharts.length; i++) {
        map.removeLayer(pieCharts[i]);
    }
    pieCharts = [];
}

document.getElementById('addCountryBtn').addEventListener('click', function() {
    addCountry = true;
});

function drawChart() {
    var data = google.visualization.arrayToDataTable(countryData);

    var options = {
        chart: {
            title: 'Energiemix',
            subtitle: 'Aufteilung der Energiequellen',
        },
        hAxis: {title: 'Energieträger'},
        vAxis: {title: 'Prozentsatz', format: 'decimal'},
        height: 400,
    };

    var chart = new google.visualization.BarChart(document.getElementById('bar_chart_div'));

    chart.draw(data, options);
}

function drawPieChart(countryData) {
    var data = google.visualization.arrayToDataTable(countryData);

    var options = {
        title: 'Energiemix',
        is3D: true,
    };

    var chart = new google.visualization.PieChart(document.getElementById('pie_chart_div'));

    chart.draw(data, options);
}



function onEachFeature(feature, layer) {
    var name = feature.properties.name_de;
    var center_lon = feature.properties.center_lon;
    var center_lat = feature.properties.center_lat;

    var year_c = feature.properties.year_c ? JSON.parse(feature.properties.year_c) : null;
    var year_g = feature.properties.year_g ? JSON.parse(feature.properties.year_g) : null;
    var year_o = feature.properties.year_o ? JSON.parse(feature.properties.year_o) : null;
    var year_n = feature.properties.year_n ? JSON.parse(feature.properties.year_n) : null;
    var year_h = feature.properties.year_h ? JSON.parse(feature.properties.year_h) : null;
    var year_s = feature.properties.year_s ? JSON.parse(feature.properties.year_s) : null;
    var year_w = feature.properties.year_w ? JSON.parse(feature.properties.year_w) : null;

    var coal = feature.properties.coal ? JSON.parse(feature.properties.coal) : null;
    var gas = feature.properties.gas ? JSON.parse(feature.properties.gas) : null;
    var oil = feature.properties.oil ? JSON.parse(feature.properties.oil) : null;
    var nuclear = feature.properties.nuclear ? JSON.parse(feature.properties.nuclear) : null;
    var hydro = feature.properties.hydro ? JSON.parse(feature.properties.hydro) : null;
    var solar = feature.properties.solar ? JSON.parse(feature.properties.solar) : null;
    var wind = feature.properties.wind ? JSON.parse(feature.properties.wind) : null;

    var coalIndex = year_c ? year_c.indexOf(selectedYear) : -1;
    var gasIndex = year_g ? year_g.indexOf(selectedYear) : -1;
    var oilIndex = year_o ? year_o.indexOf(selectedYear) : -1;
    var nuclearIndex = year_n ? year_n.indexOf(selectedYear) : -1;
    var hydroIndex = year_h ? year_h.indexOf(selectedYear) : -1;
    var solarIndex = year_s ? year_s.indexOf(selectedYear) : -1;
    var windIndex = year_w ? year_w.indexOf(selectedYear) : -1;

    var data = [];
    var labels = [];
    
    var bgColors = [];

    if (coalIndex >= 0 && coal[coalIndex]) {
        data.push(coal[coalIndex]);
        labels.push('Kohle');
        bgColors.push(colors['Kohle']);
    }
    if (gasIndex >= 0 && gas[gasIndex]) {
        data.push(gas[gasIndex]);
        labels.push('Gas');
        bgColors.push(colors['Gas']);
    }
    if (oilIndex >= 0 && oil[oilIndex]) {
        data.push(oil[oilIndex]);
        labels.push('Öl');
        bgColors.push(colors['Öl']);
    }
    if (nuclearIndex >= 0 && nuclear[nuclearIndex]) {
        data.push(nuclear[nuclearIndex]);
        labels.push('Nuklear');
        bgColors.push(colors['Nuklear']);
    }
    if (hydroIndex >= 0 && hydro[hydroIndex]) {
        data.push(hydro[hydroIndex]);
        labels.push('Hydro');
        bgColors.push(colors['Hydro']);
    }
    if (solarIndex >= 0 && solar[solarIndex]) {
        data.push(solar[solarIndex]);
        labels.push('Solar');
        bgColors.push(colors['Solar']);
    }
    if (windIndex >= 0 && wind[windIndex]) {
        data.push(wind[windIndex]);
        labels.push('Wind');
        bgColors.push(colors['Wind']);
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
        var countryDataPie = [['Energiequelle', 'Prozentsatz']];
    
        if (data.length > 0) {
            if (addCountry) {
                countryData[0].push(name);
                for (let i = 0; i < labels.length; i++) {
                    if (countryData[i + 1]) {
                        countryData[i + 1].push(data[i]);
                    } else {
                        countryData.push([labels[i], data[i]]);
                    }
    
                    if (countryDataPie[i + 1]) {
                        countryDataPie[i + 1].push(data[i]);
                    } else {
                        countryDataPie.push([labels[i], data[i]]);
                    }
                }
                addCountry = false;
            } else {
                countryData = [['Energiequelle', name]];
                for (let i = 0; i < labels.length; i++) {
                    countryData.push([labels[i], data[i]]);
                    countryDataPie.push([labels[i], data[i]]);
                }
            }
            
            drawChart();
            drawPieChart(countryDataPie);
        }
    });    

    var popupContent = `
        <strong>${name}</strong><br/>
        ${labels.map((label, index) => `${label}: ${parseFloat(data[index].toFixed(2))}%`).join('<br/>')}
    `;
    layer.bindPopup(popupContent);
}


map.addLayer(energyLayer);

var baseMaps = {
    "Google Satellit": googleSat,
    "Google Streets": googleStreets,
    "OSM Map": osmMapnik,     
};

var overlayMaps = {
    "Energiemix": energyLayer, 
};

L.control.layers(baseMaps, overlayMaps).addTo(map);

var legend = L.control({position: 'bottomright'});

legend.onAdd = function (map) {
    var div = L.DomUtil.create('div', 'info legend');
    labels.forEach((label) => {
        div.innerHTML += 
        '<i style="background:' + colors[label] + '"></i> ' +
        (label ? label + '<br>' : '+');
    });

    return div;
};

legend.addTo(map);