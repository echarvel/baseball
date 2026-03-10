import React, { useEffect, useState } from 'react';

import {validateId, validateCountryCode, sanitizeInput} from "../utils";
import useDatatFetcher from "../utils/DataFetcher";
import fetchData from "../utils/DataFetcher";

function PlayerResults() {

    const [players, setPlayers] = useState([]);


    useEffect(() => {

        fetchData()
            .then(data => {
                const subsetOfPlayers = data.players.slice(0,10);
                setPlayers(subsetOfPlayers)
                console.log(subsetOfPlayers)
            })
    }, []);

    const handleSearchById = (input) => {

        if (validateId(input)) {
            // do something
        }
    }

    const handleSearchByCountry = (input) => {

        if (validateCountryCode(input)) {
            // do something
        }
    }

 return (
     <div className="player-results">
         <div className="player-results-header">
             <div className="player-results-search">
                 <label>Player id:</label>
                 <input type=""/>
                 <button onClick={handleSearchById}>Submit</button>
             </div>
             <div className="player-results-search">
                 <label >Player Country Code:</label>
                 <input type=""/>
                 <button onClick={()=>{}}>Submit</button>
             </div>
         </div>
         <div className="players-results-section">
             {/* Body of results should go here */}
             {players.map((player) => {
                 return(
                     <div style={{"display": "flex", "gap": "1vh"}}>
                        <div>{player.playerId}</div>
                        <div>{player.birthCountry}</div>
                     </div>
                 )
             })}
         </div>


    </div>
 )
}

export default PlayerResults;
