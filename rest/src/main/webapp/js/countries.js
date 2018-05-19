/*
 * This file is part of ConfigHub.
 *
 * ConfigHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ConfigHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ConfigHub.  If not, see <http://www.gnu.org/licenses/>.
 */

var countryConfig = {
        create: false,
        valueField: 'name',
        labelField: 'name',
        optgroupField: 'continent',
        searchField: ['name'],
        delimiter: ',',
        placeholder: '',
        closeAfterSelect: false,
        openOnFocus: true,
        maxItems: 1,
        optgroups: [
            {value: 'North America', label: 'North America'},
            {value: 'South America', label: 'South America'},
            {value: 'Europe', label: 'Europe'},
            {value: 'Asia', label: 'Asia'},
            {value: 'Africa', label: 'Africa'},
            {value: 'Antarctica', label: 'Antarctica'},
            {value: 'Oceania', label: 'Oceania'}
        ],
        render: {
            optgroup_header: function (data, escape)
            {
                return '<div class="optgroup-header">' + escape(data.label) + '</div>';
            }
        }
    },
    countries = [
        {
            "name": "Andorra",
            "continent": "Europe"
        },
        {
            "name": "United Arab Emirates",
            "continent": "Asia"
        },
        {
            "name": "Afghanistan",
            "continent": "Asia"
        },
        {
            "name": "Antigua and Barbuda",
            "continent": "North America"
        },
        {
            "name": "Anguilla",
            "continent": "North America"
        },
        {
            "name": "Albania",
            "continent": "Europe"
        },
        {
            "name": "Armenia",
            "continent": "Asia"
        },
        {
            "name": "Angola",
            "continent": "Africa"
        },
        {
            "name": "Antarctica",
            "continent": "Antarctica"
        },
        {
            "name": "Argentina",
            "continent": "South America"
        },
        {
            "name": "American Samoa",
            "continent": "Oceania"
        },
        {
            "name": "Austria",
            "continent": "Europe"
        },
        {
            "name": "Australia",
            "continent": "Oceania"
        },
        {
            "name": "Aruba",
            "continent": "North America"
        },
        {
            "name": "Åland",
            "continent": "Europe"
        },
        {
            "name": "Azerbaijan",
            "continent": "Asia"
        },
        {
            "name": "Bosnia and Herzegovina",
            "continent": "Europe"
        },
        {
            "name": "Barbados",
            "continent": "North America"
        },
        {
            "name": "Bangladesh",
            "continent": "Asia"
        },
        {
            "name": "Belgium",
            "continent": "Europe"
        },
        {
            "name": "Burkina Faso",
            "continent": "Africa"
        },
        {
            "name": "Bulgaria",
            "continent": "Europe"
        },
        {
            "name": "Bahrain",
            "continent": "Asia"
        },
        {
            "name": "Burundi",
            "continent": "Africa"
        },
        {
            "name": "Benin",
            "continent": "Africa"
        },
        {
            "name": "Saint Barthélemy",
            "continent": "North America"
        },
        {
            "name": "Bermuda",
            "continent": "North America"
        },
        {
            "name": "Brunei",
            "continent": "Asia"
        },
        {
            "name": "Bolivia",
            "continent": "South America"
        },
        {
            "name": "Bonaire",
            "continent": "North America"
        },
        {
            "name": "Brazil",
            "continent": "South America"
        },
        {
            "name": "Bahamas",
            "continent": "North America"
        },
        {
            "name": "Bhutan",
            "continent": "Asia"
        },
        {
            "name": "Bouvet Island",
            "continent": "Antarctica"
        },
        {
            "name": "Botswana",
            "continent": "Africa"
        },
        {
            "name": "Belarus",
            "continent": "Europe"
        },
        {
            "name": "Belize",
            "continent": "North America"
        },
        {
            "name": "Canada",
            "continent": "North America"
        },
        {
            "name": "Cocos [Keeling] Islands",
            "continent": "Asia"
        },
        {
            "name": "Democratic Republic of the Congo",
            "continent": "Africa"
        },
        {
            "name": "Central African Republic",
            "continent": "Africa"
        },
        {
            "name": "Republic of the Congo",
            "continent": "Africa"
        },
        {
            "name": "Switzerland",
            "continent": "Europe"
        },
        {
            "name": "Ivory Coast",
            "continent": "Africa"
        },
        {
            "name": "Cook Islands",
            "continent": "Oceania"
        },
        {
            "name": "Chile",
            "continent": "South America"
        },
        {
            "name": "Cameroon",
            "continent": "Africa"
        },
        {
            "name": "China",
            "continent": "Asia"
        },
        {
            "name": "Colombia",
            "continent": "South America"
        },
        {
            "name": "Costa Rica",
            "continent": "North America"
        },
        {
            "name": "Cuba",
            "continent": "North America"
        },
        {
            "name": "Cape Verde",
            "continent": "Africa"
        },
        {
            "name": "Curacao",
            "continent": "North America"
        },
        {
            "name": "Christmas Island",
            "continent": "Asia"
        },
        {
            "name": "Cyprus",
            "continent": "Europe"
        },
        {
            "name": "Czech Republic",
            "continent": "Europe"
        },
        {
            "name": "Germany",
            "continent": "Europe"
        },
        {
            "name": "Djibouti",
            "continent": "Africa"
        },
        {
            "name": "Denmark",
            "continent": "Europe"
        },
        {
            "name": "Dominica",
            "continent": "North America"
        },
        {
            "name": "Dominican Republic",
            "continent": "North America"
        },
        {
            "name": "Algeria",
            "continent": "Africa"
        },
        {
            "name": "Ecuador",
            "continent": "South America"
        },
        {
            "name": "Estonia",
            "continent": "Europe"
        },
        {
            "name": "Egypt",
            "continent": "Africa"
        },
        {
            "name": "Western Sahara",
            "continent": "Africa"
        },
        {
            "name": "Eritrea",
            "continent": "Africa"
        },
        {
            "name": "Spain",
            "continent": "Europe"
        },
        {
            "name": "Ethiopia",
            "continent": "Africa"
        },
        {
            "name": "Finland",
            "continent": "Europe"
        },
        {
            "name": "Fiji",
            "continent": "Oceania"
        },
        {
            "name": "Falkland Islands",
            "continent": "South America"
        },
        {
            "name": "Micronesia",
            "continent": "Oceania"
        },
        {
            "name": "Faroe Islands",
            "continent": "Europe"
        },
        {
            "name": "France",
            "continent": "Europe"
        },
        {
            "name": "Gabon",
            "continent": "Africa"
        },
        {
            "name": "United Kingdom",
            "continent": "Europe"
        },
        {
            "name": "Grenada",
            "continent": "North America"
        },
        {
            "name": "Georgia",
            "continent": "Asia"
        },
        {
            "name": "French Guiana",
            "continent": "South America"
        },
        {
            "name": "Guernsey",
            "continent": "Europe"
        },
        {
            "name": "Ghana",
            "continent": "Africa"
        },
        {
            "name": "Gibraltar",
            "continent": "Europe"
        },
        {
            "name": "Greenland",
            "continent": "North America"
        },
        {
            "name": "Gambia",
            "continent": "Africa"
        },
        {
            "name": "Guinea",
            "continent": "Africa"
        },
        {
            "name": "Guadeloupe",
            "continent": "North America"
        },
        {
            "name": "Equatorial Guinea",
            "continent": "Africa"
        },
        {
            "name": "Greece",
            "continent": "Europe"
        },
        {
            "name": "South Georgia and the South Sandwich Islands",
            "continent": "Antarctica"
        },
        {
            "name": "Guatemala",
            "continent": "North America"
        },
        {
            "name": "Guam",
            "continent": "Oceania"
        },
        {
            "name": "Guinea-Bissau",
            "continent": "Africa"
        },
        {
            "name": "Guyana",
            "continent": "South America"
        },
        {
            "name": "Hong Kong",
            "continent": "Asia"
        },
        {
            "name": "Heard Island and McDonald Islands",
            "continent": "Antarctica"
        },
        {
            "name": "Honduras",
            "continent": "North America"
        },
        {
            "name": "Croatia",
            "continent": "Europe"
        },
        {
            "name": "Haiti",
            "continent": "North America"
        },
        {
            "name": "Hungary",
            "continent": "Europe"
        },
        {
            "name": "Indonesia",
            "continent": "Asia"
        },
        {
            "name": "Ireland",
            "continent": "Europe"
        },
        {
            "name": "Israel",
            "continent": "Asia"
        },
        {
            "name": "Isle of Man",
            "continent": "Europe"
        },
        {
            "name": "India",
            "continent": "Asia"
        },
        {
            "name": "British Indian Ocean Territory",
            "continent": "Asia"
        },
        {
            "name": "Iraq",
            "continent": "Asia"
        },
        {
            "name": "Iran",
            "continent": "Asia"
        },
        {
            "name": "Iceland",
            "continent": "Europe"
        },
        {
            "name": "Italy",
            "continent": "Europe"
        },
        {
            "name": "Jersey",
            "continent": "Europe"
        },
        {
            "name": "Jamaica",
            "continent": "North America"
        },
        {
            "name": "Jordan",
            "continent": "Asia"
        },
        {
            "name": "Japan",
            "continent": "Asia"
        },
        {
            "name": "Kenya",
            "continent": "Africa"
        },
        {
            "name": "Kyrgyzstan",
            "continent": "Asia"
        },
        {
            "name": "Cambodia",
            "continent": "Asia"
        },
        {
            "name": "Kiribati",
            "continent": "Oceania"
        },
        {
            "name": "Comoros",
            "continent": "Africa"
        },
        {
            "name": "Saint Kitts and Nevis",
            "continent": "North America"
        },
        {
            "name": "North Korea",
            "continent": "Asia"
        },
        {
            "name": "South Korea",
            "continent": "Asia"
        },
        {
            "name": "Kuwait",
            "continent": "Asia"
        },
        {
            "name": "Cayman Islands",
            "continent": "North America"
        },
        {
            "name": "Kazakhstan",
            "continent": "Asia"
        },
        {
            "name": "Laos",
            "continent": "Asia"
        },
        {
            "name": "Lebanon",
            "continent": "Asia"
        },
        {
            "name": "Saint Lucia",
            "continent": "North America"
        },
        {
            "name": "Liechtenstein",
            "continent": "Europe"
        },
        {
            "name": "Sri Lanka",
            "continent": "Asia"
        },
        {
            "name": "Liberia",
            "continent": "Africa"
        },
        {
            "name": "Lesotho",
            "continent": "Africa"
        },
        {
            "name": "Lithuania",
            "continent": "Europe"
        },
        {
            "name": "Luxembourg",
            "continent": "Europe"
        },
        {
            "name": "Latvia",
            "continent": "Europe"
        },
        {
            "name": "Libya",
            "continent": "Africa"
        },
        {
            "name": "Morocco",
            "continent": "Africa"
        },
        {
            "name": "Monaco",
            "continent": "Europe"
        },
        {
            "name": "Moldova",
            "continent": "Europe"
        },
        {
            "name": "Montenegro",
            "continent": "Europe"
        },
        {
            "name": "Saint Martin",
            "continent": "North America"
        },
        {
            "name": "Madagascar",
            "continent": "Africa"
        },
        {
            "name": "Marshall Islands",
            "continent": "Oceania"
        },
        {
            "name": "Macedonia",
            "continent": "Europe"
        },
        {
            "name": "Mali",
            "continent": "Africa"
        },
        {
            "name": "Myanmar [Burma]",
            "continent": "Asia"
        },
        {
            "name": "Mongolia",
            "continent": "Asia"
        },
        {
            "name": "Macao",
            "continent": "Asia"
        },
        {
            "name": "Northern Mariana Islands",
            "continent": "Oceania"
        },
        {
            "name": "Martinique",
            "continent": "North America"
        },
        {
            "name": "Mauritania",
            "continent": "Africa"
        },
        {
            "name": "Montserrat",
            "continent": "North America"
        },
        {
            "name": "Malta",
            "continent": "Europe"
        },
        {
            "name": "Mauritius",
            "continent": "Africa"
        },
        {
            "name": "Maldives",
            "continent": "Asia"
        },
        {
            "name": "Malawi",
            "continent": "Africa"
        },
        {
            "name": "Mexico",
            "continent": "North America"
        },
        {
            "name": "Malaysia",
            "continent": "Asia"
        },
        {
            "name": "Mozambique",
            "continent": "Africa"
        },
        {
            "name": "Namibia",
            "continent": "Africa"
        },
        {
            "name": "New Caledonia",
            "continent": "Oceania"
        },
        {
            "name": "Niger",
            "continent": "Africa"
        },
        {
            "name": "Norfolk Island",
            "continent": "Oceania"
        },
        {
            "name": "Nigeria",
            "continent": "Africa"
        },
        {
            "name": "Nicaragua",
            "continent": "North America"
        },
        {
            "name": "Netherlands",
            "continent": "Europe"
        },
        {
            "name": "Norway",
            "continent": "Europe"
        },
        {
            "name": "Nepal",
            "continent": "Asia"
        },
        {
            "name": "Nauru",
            "continent": "Oceania"
        },
        {
            "name": "Niue",
            "continent": "Oceania"
        },
        {
            "name": "New Zealand",
            "continent": "Oceania"
        },
        {
            "name": "Oman",
            "continent": "Asia"
        },
        {
            "name": "Panama",
            "continent": "North America"
        },
        {
            "name": "Peru",
            "continent": "South America"
        },
        {
            "name": "French Polynesia",
            "continent": "Oceania"
        },
        {
            "name": "Papua New Guinea",
            "continent": "Oceania"
        },
        {
            "name": "Philippines",
            "continent": "Asia"
        },
        {
            "name": "Pakistan",
            "continent": "Asia"
        },
        {
            "name": "Poland",
            "continent": "Europe"
        },
        {
            "name": "Saint Pierre and Miquelon",
            "continent": "North America"
        },
        {
            "name": "Pitcairn Islands",
            "continent": "Oceania"
        },
        {
            "name": "Puerto Rico",
            "continent": "North America"
        },
        {
            "name": "Palestine",
            "continent": "Asia"
        },
        {
            "name": "Portugal",
            "continent": "Europe"
        },
        {
            "name": "Palau",
            "continent": "Oceania"
        },
        {
            "name": "Paraguay",
            "continent": "South America"
        },
        {
            "name": "Qatar",
            "continent": "Asia"
        },
        {
            "name": "Réunion",
            "continent": "Africa"
        },
        {
            "name": "Romania",
            "continent": "Europe"
        },
        {
            "name": "Serbia",
            "continent": "Europe"
        },
        {
            "name": "Russia",
            "continent": "Europe"
        },
        {
            "name": "Rwanda",
            "continent": "Africa"
        },
        {
            "name": "Saudi Arabia",
            "continent": "Asia"
        },
        {
            "name": "Solomon Islands",
            "continent": "Oceania"
        },
        {
            "name": "Seychelles",
            "continent": "Africa"
        },
        {
            "name": "Sudan",
            "continent": "Africa"
        },
        {
            "name": "Sweden",
            "continent": "Europe"
        },
        {
            "name": "Singapore",
            "continent": "Asia"
        },
        {
            "name": "Saint Helena",
            "continent": "Africa"
        },
        {
            "name": "Slovenia",
            "continent": "Europe"
        },
        {
            "name": "Svalbard and Jan Mayen",
            "continent": "Europe"
        },
        {
            "name": "Slovakia",
            "continent": "Europe"
        },
        {
            "name": "Sierra Leone",
            "continent": "Africa"
        },
        {
            "name": "San Marino",
            "continent": "Europe"
        },
        {
            "name": "Senegal",
            "continent": "Africa"
        },
        {
            "name": "Somalia",
            "continent": "Africa"
        },
        {
            "name": "Suriname",
            "continent": "South America"
        },
        {
            "name": "South Sudan",
            "continent": "Africa"
        },
        {
            "name": "São Tomé and Príncipe",
            "continent": "Africa"
        },
        {
            "name": "El Salvador",
            "continent": "North America"
        },
        {
            "name": "Sint Maarten",
            "continent": "North America"
        },
        {
            "name": "Syria",
            "continent": "Asia"
        },
        {
            "name": "Swaziland",
            "continent": "Africa"
        },
        {
            "name": "Turks and Caicos Islands",
            "continent": "North America"
        },
        {
            "name": "Chad",
            "continent": "Africa"
        },
        {
            "name": "French Southern Territories",
            "continent": "Antarctica"
        },
        {
            "name": "Togo",
            "continent": "Africa"
        },
        {
            "name": "Thailand",
            "continent": "Asia"
        },
        {
            "name": "Tajikistan",
            "continent": "Asia"
        },
        {
            "name": "Tokelau",
            "continent": "Oceania"
        },
        {
            "name": "East Timor",
            "continent": "Oceania"
        },
        {
            "name": "Turkmenistan",
            "continent": "Asia"
        },
        {
            "name": "Tunisia",
            "continent": "Africa"
        },
        {
            "name": "Tonga",
            "continent": "Oceania"
        },
        {
            "name": "Turkey",
            "continent": "Asia"
        },
        {
            "name": "Trinidad and Tobago",
            "continent": "North America"
        },
        {
            "name": "Tuvalu",
            "continent": "Oceania"
        },
        {
            "name": "Taiwan",
            "continent": "Asia"
        },
        {
            "name": "Tanzania",
            "continent": "Africa"
        },
        {
            "name": "Ukraine",
            "continent": "Europe"
        },
        {
            "name": "Uganda",
            "continent": "Africa"
        },
        {
            "name": "U.S. Minor Outlying Islands",
            "continent": "Oceania"
        },
        {
            "name": "United States",
            "continent": "North America"
        },
        {
            "name": "Uruguay",
            "continent": "South America"
        },
        {
            "name": "Uzbekistan",
            "continent": "Asia"
        },
        {
            "name": "Vatican City",
            "continent": "Europe"
        },
        {
            "name": "Saint Vincent and the Grenadines",
            "continent": "North America"
        },
        {
            "name": "Venezuela",
            "continent": "South America"
        },
        {
            "name": "British Virgin Islands",
            "continent": "North America"
        },
        {
            "name": "U.S. Virgin Islands",
            "continent": "North America"
        },
        {
            "name": "Vietnam",
            "continent": "Asia"
        },
        {
            "name": "Vanuatu",
            "continent": "Oceania"
        },
        {
            "name": "Wallis and Futuna",
            "continent": "Oceania"
        },
        {
            "name": "Samoa",
            "continent": "Oceania"
        },
        {
            "name": "Kosovo",
            "continent": "Europe"
        },
        {
            "name": "Yemen",
            "continent": "Asia"
        },
        {
            "name": "Mayotte",
            "continent": "Africa"
        },
        {
            "name": "South Africa",
            "continent": "Africa"
        },
        {
            "name": "Zambia",
            "continent": "Africa"
        },
        {
            "name": "Zimbabwe",
            "continent": "Africa"
        }
    ];