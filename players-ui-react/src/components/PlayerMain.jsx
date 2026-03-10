import logo from '../assets/logo.svg';
import '../styling/PlayersMain.css';
import PlayerResults from "./PlayersResults";

function PlayerMain() {
    return (
        <div className="players-main">
            <header className="players-header">
                <img src={logo} className="players-logo" alt="logo" />
                <p>
                    Hello Players
                </p>
            </header>
            <PlayerResults/>
        </div>
    );
}

export default PlayerMain;
