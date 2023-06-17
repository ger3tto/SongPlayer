package com.github.hhhzzzsss.songplayer;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
    public static void createDirectoriesSilently(Path path) {
        try {
            Files.createDirectories(path);
        }
        catch (IOException e) {}
    }

    public static Stream<Path> listFilesSilently(Path path) {
        try {
            return Files.list(path);
        }
        catch (IOException e) {
            return null;
        }
    }

    public static String formatTime(long milliseconds) {
        long temp = Math.abs(milliseconds);
        temp /= 1000;
        long seconds = temp % 60;
        temp /= 60;
        long minutes = temp % 60;
        temp /= 60;
        long hours = temp;
        StringBuilder sb = new StringBuilder();
        if (milliseconds < 0) {
            sb.append("-");
        }
        if (hours > 0) {
            sb.append(String.format("%d:", hours));
            sb.append(String.format("%02d:", minutes));
        } else {
            sb.append(String.format("%d:", minutes));
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
    }

    public static Pattern timePattern = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");
    public static long parseTime(String timeStr) throws IOException {
        Matcher matcher = timePattern.matcher(timeStr);
        if (matcher.matches()) {
            long time = 0;
            String hourString = matcher.group(1);
            String minuteString = matcher.group(2);
            String secondString = matcher.group(3);
            if (hourString != null) {
                time += Integer.parseInt(hourString) * 60 * 60 * 1000;
            }
            time += Integer.parseInt(minuteString) * 60 * 1000;
            time += Double.parseDouble(secondString) * 1000.0;
            return time;
        } else {
            throw new IOException("Invalid time pattern");
        }
    }

    public static CompletableFuture<Suggestions> giveSongSuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString = "";
        Path dir = SongPlayer.SONG_DIR;
        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash+1);
            dir = dir.resolve(dirString);
        }

        Stream<Path> songFiles = listFilesSilently(dir);
        if (songFiles == null) return null;

        ArrayList<String> suggestions = new ArrayList<>();
        for (Path path : songFiles.collect(Collectors.toList())) {
            if (Files.isRegularFile(path)) {
                suggestions.add(dirString + path.getFileName().toString());
            }
            else if (Files.isDirectory(path)) {
                suggestions.add(dirString + path.getFileName().toString() + "/");
            }
        }
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static CompletableFuture<Suggestions> givePlaylistSuggestions(SuggestionsBuilder suggestionsBuilder) {
        if (!Files.exists(SongPlayer.PLAYLISTS_DIR)) return null;
        return CommandSource.suggestMatching(
                listFilesSilently(SongPlayer.PLAYLISTS_DIR)
                        .filter(Files::isDirectory)
                        .map(Path::getFileName)
                        .map(Path::toString),
                suggestionsBuilder);
    }

    public static CompletableFuture<Suggestions> giveSongDirectorySuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString;
        Path dir = SongPlayer.SONG_DIR;
        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash+1);
            dir = dir.resolve(dirString);
        }
        else {
            dirString = "";
        }

        Stream<Path> songFiles = listFilesSilently(dir);
        if (songFiles == null) return null;

        List<String> suggestions = songFiles
                .filter(Files::isDirectory)
                .map(path -> dirString + path.getFileName().toString() + "/")
                .collect(Collectors.toList());
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static MutableText getStyledText(String str, Style style) {
        MutableText text = MutableText.of(new LiteralTextContent(str));
        text.setStyle(style);
        return text;
    }

    public static void setItemName(ItemStack stack, Text text) {
        stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY).putString(ItemStack.NAME_KEY, Text.Serializer.toJson(text));
    }

    public static void setItemLore(ItemStack stack, Text... loreLines) {
        NbtList lore = new NbtList();
        for (Text line : loreLines) {
            lore.add(NbtString.of(Text.Serializer.toJson(line)));
        }
        stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY).put(ItemStack.LORE_KEY, lore);
    }
}
