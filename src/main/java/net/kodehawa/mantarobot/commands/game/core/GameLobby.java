/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.interaction.Lobby;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GameLobby extends Lobby {

    public static final Map<TextChannel, GameLobby> LOBBYS = new ConcurrentHashMap<>();

	@Getter
	GuildMessageReceivedEvent event;
	@Getter
	LinkedList<Game> gamesToPlay;
	@Getter
	Guild guild;
	@Getter
	List<String> players;

	public GameLobby(GuildMessageReceivedEvent event, List<String> players, LinkedList<Game> games) {
		super(event.getChannel());
		this.guild = event.getGuild();
		this.event = event;
		this.players = players;
		this.gamesToPlay = games;
	}

    @Override
    public String toString() {
        return String.format("GameLobby{%s, %s, players:%d, channel:%s}", event.getGuild(), gamesToPlay, players.size(), getChannel());
    }

    public void startFirstGame() {
        LOBBYS.put(event.getChannel(), this);
        if(gamesToPlay.getFirst().onStart(this)) {
            gamesToPlay.getFirst().call(this, players);
        } else {
            LOBBYS.remove(getChannel());
            gamesToPlay.clear();
        }
    }

    public void startNextGame() {
        gamesToPlay.removeFirst();
        try {
            if(gamesToPlay.getFirst().onStart(this)) {
                gamesToPlay.getFirst().call(this, players);
            } else {
                gamesToPlay.clear();
                LOBBYS.remove(getChannel());
            }
        } catch(Exception e) {
            LOBBYS.remove(getChannel());
        }
    }
}