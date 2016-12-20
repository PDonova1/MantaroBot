package net.kodehawa.mantarobot.cmd.osu;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.osu.api.ciyfhx.Mod;
import com.osu.api.ciyfhx.OsuClient;
import com.osu.api.ciyfhx.User;
import com.osu.api.ciyfhx.UserScore;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.util.Utils;

public class Osu extends Command {

	private OsuClient osuClient = null;
	private Map<String, Object> map = new HashMap<>();
	private Map<String, String> values = new HashMap<>();
	
	public Osu()
	{
		setName("osu");
		setDescription("Retrieves osu! related things. Use the help argument to get details.");
		setCommandType("user");
		setExtendedHelp(
				"Retrieves information from the osu!api.\n"
				+ "Usage: \n"
				+ "~>osu best player mode: Retrieves best scores of the user specified in the specified gamemode.\n"
				+ "~>osu recent player mode: Retrieves recent scores of the user specified in the specified gamemode.\n"
				+ "~>osu user player: Retrieves information about a osu! player.\n"
				+ "Parameter description:\n"
				+ "*player*: The osu! player to look info for.\n"
				+ "*mode*: Mode to look for. Possible values are: standard, taiko, mania and ctb.\n"
				);
		
		//From a human input, translate to API values.
		values.put("standard", "0");
		values.put("taiko", "1");
		values.put("mania", "2");
		values.put("ctb", "3");

	}

	@Override
	public void onCommand(String[] message, String content, MessageReceivedEvent evt) {
		osuClient = new OsuClient(Mantaro.instance().getConfig().values().get("osuapikey").toString());
		String noArgs = content.split(" ")[0];

		switch(noArgs){
		case "best":
			evt.getChannel().sendMessage(":speech_balloon: Retrieving information from osu! server...").queue(sentMessage ->
			{
				sentMessage.editMessage(best(content)).queue();
			});
			break;
		case "recent":
			evt.getChannel().sendMessage(":speech_balloon: Retrieving information from server...").queue(sentMessage ->
			{
				sentMessage.editMessage(recent(content)).queue();
			});
			break;
		case "user":
			evt.getChannel().sendMessage(user(content)).queue();
			break;
		default:
			evt.getChannel().sendMessage(getExtendedHelp()).queue();
			break;
		}
	}
		
	private String best(String beheadedMessage){
		String finalResponse;
		try{
			boolean requiresMode = false;
			if(beheadedMessage.length() > 10){
				requiresMode = true;
			}
			long start = System.currentTimeMillis();
			String beheaded1 = beheadedMessage.replace("best ", "");
			String[] args = beheaded1.split(" ");

			if(requiresMode){
				map.put("m", values.get(args[1]));
			} else {
				map.put("m", 0);
			}

			User hey = osuClient.getUser(args[0], map);
			List<UserScore> userBest = osuClient.getUserBest(hey, map);
			StringBuilder sb = new StringBuilder();
			List<String> best = new CopyOnWriteArrayList<>();
			
			int n = -1;
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			for (@SuppressWarnings("unused") UserScore u : userBest){
				n++;
				n1++;
				String mods1 = "";
				try{
					if(userBest.get(n).getEnabledMods().size() > 0){
						List<Mod> mods = userBest.get(n).getEnabledMods();
						int i = 0;
						StringBuilder sb1 = new StringBuilder();
						for (@SuppressWarnings("unused") Mod mod : mods){
							sb1.append(Utils.instance().getMod(mods.get(i)));
							i++;
						}
						mods1 = " / Mods: " + sb1.toString();
					}
				} catch(ArrayIndexOutOfBoundsException ignored){}
				
				best.add(n1 + ".- " + userBest.get(n).getBeatMap().getTitle().replace("'", "") + 
						" (\u2605"  + df.format(userBest.get(n).getBeatMap().getDifficultyRating()) + ") - " + userBest.get(n).getBeatMap().getCreator() 
						+ mods1
						+ "\n   Date: " + userBest.get(n).getDate() + " ~ Max Combo: " + userBest.get(n).getMaxCombo() +
						" ~ PP: " + df.format(userBest.get(n).getPP()) + " ~ Rank: " + userBest.get(n).getRank()  + "\n");
				sb.append(best.get(n));
			}
			long end = System.currentTimeMillis() - start;
		    finalResponse = "```ruby\n" + sb.toString() + " \nResponse time: " + end + "ms```";
		} catch(Exception e){
			e.printStackTrace();
			finalResponse = ":heavy_multiplication_x: Error retrieving results or no results found.";
		}
		
		return finalResponse;
	}
	
	private String recent(String beheadedMessage){
		String finalMessage;
		try{
			long start = System.currentTimeMillis();
			String beheaded1 = beheadedMessage.replace("recent ", "");
			String[] args = beheaded1.split(" ");
			map.put("m", values.get(args[1]));
			User hey = osuClient.getUser(args[0], map);
			List<UserScore> userRecent = osuClient.getUserRecent(hey, map);
			StringBuilder sb = new StringBuilder();
			List<String> recent = new CopyOnWriteArrayList<>();
			 
			int n = -1;
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			String mods1 = "";
			for (@SuppressWarnings("unused") UserScore u : userRecent){
				try{
					if(userRecent.get(n).getEnabledMods().size() > 0){
						List<Mod> mods = userRecent.get(n).getEnabledMods();
						int i = 0;
						StringBuilder sb1 = new StringBuilder();
						
						for (@SuppressWarnings("unused") Mod mod : mods){
							sb1.append(Utils.instance().getMod(mods.get(i)));
							i++;
						}
						mods1 = " / Mods: " + sb1.toString();
					}
				} catch(ArrayIndexOutOfBoundsException ignored){}
				
				n1++;
				recent.add(n1 + ".- " + userRecent.get(n).getBeatMap().getTitle().replace("'", "") + " (\u2605"  
						+ df.format(userRecent.get(n).getBeatMap().getDifficultyRating()) + ") - " + userRecent.get(n).getBeatMap().getCreator()
						+ mods1
						+ "\n Date: " + userRecent.get(n).getDate() + " ~ Max Combo: " + userRecent.get(n).getMaxCombo() +
						"\n");
				
				sb.append(recent.get(n));
				n++;
			}
			long end = System.currentTimeMillis() - start;
			finalMessage = "```ruby\n" + sb.toString() + " \nResponse time: " + end + "ms```";
		} catch (Exception e){
			e.printStackTrace();
			finalMessage = ":heavy_multiplication_x: Error retrieving results or no results found.";
		}
		return finalMessage;
	}
	
	private MessageEmbed user(String beheadedMessage){
		MessageEmbed finalMessage;
		try{
			long start = System.currentTimeMillis();
			String beheaded1 = beheadedMessage.replace("user ", "");
			
			String[] args = beheaded1.split(" ");
			
			map.put("m", 0);
			
			User osuClientUser = osuClient.getUser(args[0], map);
			DecimalFormat dfa = new DecimalFormat("####0.00"); //For accuracy
			DecimalFormat df = new DecimalFormat("####0"); //For everything else
			long end = System.currentTimeMillis() - start;
			EmbedBuilder builder = new EmbedBuilder();
					builder.setAuthor("osu! statistics for " + osuClientUser.getUsername(), "https://osu.ppy.sh/" + osuClientUser.getUserID(), "https://a.ppy.sh/" + osuClientUser.getUserID())
					.setColor(Color.GRAY)
					.addField("Rank", "#" + df.format(osuClientUser.getPPRank()), true)
					.addField(":flag_" + osuClientUser.getCountry().toLowerCase() + ": Country Rank", "#" + df.format(osuClientUser.getPPCountryRank()), true)
					.addField("PP", df.format(osuClientUser.getPPRaw()) + "pp", true)
					.addField("Accuracy", dfa.format(osuClientUser.getAccuracy()) + "%", true)
					.addField("Level", df.format(osuClientUser.getLevel()), true)
					.addField("Ranked Score", df.format(osuClientUser.getRankedScore()), true)
					.addField("SS", df.format(osuClientUser.getCountRankSS()), true)
					.addField("S", df.format(osuClientUser.getCountRankS()), true)
					.addField("A", df.format(osuClientUser.getCountRankA()), true)
					.setFooter("Response time: " + end + "ms.", null);
			finalMessage = builder.build();
		} catch (Exception e){
			e.printStackTrace();
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("Error.")
					.setColor(Color.RED)
					.addField("Description", "Error retrieving results or no results found.", false);
			finalMessage = builder.build();
		}
		return finalMessage;
	}
}
