package net.kodehawa.mantarobot.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.utils.AnimeData;
import net.kodehawa.mantarobot.commands.utils.CharacterData;
import net.kodehawa.mantarobot.core.listeners.FunctionListener;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.GeneralUtils;
import net.kodehawa.mantarobot.utils.GsonDataManager;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.stream.Collectors;

public class AnimeCmds extends Module {
	public static Logger LOGGER = LoggerFactory.getLogger("AnimeCmds");
	private final String CLIENT_SECRET = MantaroData.getConfig().get().alsecret;
	private String authToken;

	public AnimeCmds() {
		super(Category.FUN);
		anime();
		character();
		login();
	}

	private void anime() {
		super.register("anime", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					//Set variables to use later. They will be parsed to JSON later on.
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s",
						URLEncoder.encode(content, "UTF-8"), authToken);
					String json = GeneralUtils.instance().getObjectFromUrl(connection, event);
					AnimeData[] type = GsonDataManager.GSON.fromJson(json, AnimeData[].class);
					EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Anime selection. Type a number to continue.").setFooter("This timeouts in 10 seconds.", null);
					StringBuilder b = new StringBuilder();
					for (int i = 0; i < 4 && i < type.length; i++) {
						AnimeData animeData = type[i];
						if(animeData != null)
							b.append('[').append(i + 1).append("] ").append(animeData.title_english).append(" (").append(animeData.title_japanese).append(")").append("\n");
					}
					event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();

					FunctionListener functionListener = new FunctionListener(event.getChannel().getId(), (l, e) -> {
						if (!e.getAuthor().equals(event.getAuthor())) return false;

						try {
							int choose = Integer.parseInt(e.getMessage().getContent());
							if (choose < 1 || choose > type.length) return false;
							animeData(e, type, choose - 1);
							return true;
						} catch (Exception ex) {
							event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + ex.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + ex.getMessage() + "``").queue();
						}
						return false;
					});

					MantaroBot.getJDA().addEventListener(functionListener);
					Async.asyncSleepThen(10000, () -> {
						if (!functionListener.isDone()) {
							MantaroBot.getJDA().removeEventListener(functionListener);
							event.getChannel().sendMessage("\u274C Timeout: No reply in 10 seconds").queue();
						}
					}).run();
				} catch (Exception e) {
					event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Anime command")
					.setDescription("Retrieves anime info from **AniList** (For anime characters use ~>character).\n"
						+ "Usage: \n"
						+ "~>anime [animename]: Gets information of an anime based on parameters.\n"
						+ "Parameter description:\n"
						+ "[animename]: The name of the anime you are looking for. Make sure to write it similar to the original english name.\n")
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	private void character() {
		super.register("character", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				try {
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = GeneralUtils.instance().getObjectFromUrl(url, event);
					CharacterData[] character = GsonDataManager.GSON.fromJson(json, CharacterData[].class);
					EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Character selection. Type a number to continue.").setFooter("This timeouts in 10 seconds.", null);
					StringBuilder b = new StringBuilder();

					for (int i = 0; i < 4 && i < character.length; i++) {
						CharacterData characterData = character[i];
						if(characterData != null)
							b.append('[').append(i + 1).append("] ").append(characterData.name_first).append(" ").append(characterData.name_last).append("\n");
					}
					channel.sendMessage(builder.setDescription(b.toString()).build()).queue();

					FunctionListener functionListener = new FunctionListener(event.getChannel().getId(), (l, e) -> {
						if (!e.getAuthor().equals(event.getAuthor())) return false;

						try {
							int choose = Integer.parseInt(e.getMessage().getContent());
							if (choose < 1 || choose > character.length) return false;
							characterData(e, character, choose - 1);
							return true;
						} catch (Exception e1) {
							event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e1.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e1.getMessage() + "``").queue();
						}
						return false;
					});

					MantaroBot.getJDA().addEventListener(functionListener);
					Async.asyncSleepThen(10000, () -> {
						if (!functionListener.isDone()) {
							MantaroBot.getJDA().removeEventListener(functionListener);
							event.getChannel().sendMessage("\u274C Timeout: No reply in 10 seconds").queue();
						}
					}).run();

				} catch (Exception e) {
					LOGGER.warn("Problem processing data.", e);
					event.getChannel().sendMessage("**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "AnimeCmds character command")
					.setDescription("Retrieves character info from **AniList**.\n"
						+ "Usage: \n"
						+ "~>character [charname]: Gets information of a character based on parameters.\n"
						+ "Parameter description:\n"
						+ "[character]: The name of the character you are looking info of. Make sure to write the exact character name or close to it.\n")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	private void animeData(GuildMessageReceivedEvent event, AnimeData[] type, int pick){
		String ANIME_TITLE = type[pick].title_english;
		String RELEASE_DATE = StringUtils.substringBefore(type[pick].start_date, "T");
		String END_DATE = StringUtils.substringBefore(type[pick].end_date, "T");
		String ANIME_DESCRIPTION = type[pick].description.replaceAll("<br>", "\n");
		String AVERAGE_SCORE = type[pick].average_score;
		String IMAGE_URL = type[pick].image_url_lge;
		String TYPE = GeneralUtils.capitalize(type[pick].series_type);
		String EPISODES = type[pick].total_episodes.toString();
		String DURATION = type[pick].duration.toString();
		String GENRES = type[pick].genres.stream().collect(Collectors.joining(", "));

		//Start building the embedded message.
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
				.setAuthor("Anime information for " + ANIME_TITLE, "http://anilist.co/anime/"
						+ type[0].id, type[0].image_url_sml)
				.setFooter("Information provided by AniList", null)
				.setThumbnail(IMAGE_URL)
				.addField("Description: ", ANIME_DESCRIPTION.length() <= 1024 ? ANIME_DESCRIPTION : ANIME_DESCRIPTION.substring(0, 1020) + "...", false)
				.addField("Release date: ", RELEASE_DATE, true)
				.addField("End date: ", END_DATE, true)
				.addField("Average score: ", AVERAGE_SCORE + "/100", true)
				.addField("Type", TYPE, true)
				.addField("Episodes", EPISODES, true)
				.addField("Episode Duration", DURATION + " minutes.", true)
				.addField("Genres", GENRES, false);
		event.getChannel().sendMessage(embed.build()).queue();
	}

	private void characterData(GuildMessageReceivedEvent event, CharacterData[] character, int pick){
		String CHAR_NAME = character[pick].name_first + " " + character[pick].name_last + "\n(" + character[0].name_japanese + ")";
		String ALIASES = character[pick].name_alt == null ? "No aliases" : "Also known as: " + character[0].name_alt;
		String IMAGE_URL = character[pick].image_url_med;
		String CHAR_DESCRIPTION = character[pick].info.isEmpty() ? "No info."
				: character[pick].info.length() <= 1024 ? character[pick].info : character[pick].info.substring(0, 1020 - 1) + "...";
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
				.setThumbnail(IMAGE_URL)
				.setAuthor("Information for " + CHAR_NAME, "http://anilist.co/character/" + character[0].id, IMAGE_URL)
				.setDescription(ALIASES)
				.addField("Information", CHAR_DESCRIPTION, true)
				.setFooter("Information provided by AniList", null);

		event.getChannel().sendMessage(embed.build()).queue();
	}

	/**
	 * Refreshes the already given token in x ms. Usually every 58 minutes.
	 *
	 * Gives the new AniList access token.
	 */
	private void login() {
		Async.startAsyncTask("AniList Login Task", this::authenticate, 	3500);
	}

	/**
	 * @return The new AniList access token.
	 */
	private void authenticate() {
		URL aniList;
		try {
			aniList = new URL("https://anilist.co/api/auth/access_token");
			HttpURLConnection alc = (HttpURLConnection) aniList.openConnection();
			alc.setRequestMethod("POST");
			alc.setRequestProperty("User-Agent", "Mantaro");
			alc.setDoOutput(true);
			alc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			OutputStreamWriter osw = new OutputStreamWriter(alc.getOutputStream());
			String CLIENT_ID = "kodehawa-o43eq";
			osw.write("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET);
			osw.flush();
			InputStream inputstream = alc.getInputStream();
			String json = CharStreams.toString(new InputStreamReader(inputstream, Charsets.UTF_8));
			JSONObject jObject = new JSONObject(json);
			authToken = jObject.getString("access_token");
			LOGGER.info("Updated auth token.");
		} catch (Exception e) {
			LOGGER.warn("Problem while updating auth token! " + e.getCause() + " " + e.getMessage());
			if (MantaroData.getConfig().get().debug) {
				e.printStackTrace();
			}
		}
	}
}
