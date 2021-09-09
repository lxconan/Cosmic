/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.quest;

import client.Character;
import client.QuestStatus;
import client.QuestStatus.Status;
import config.YamlConfig;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.MapleDataTool;
import provider.wz.WZFiles;
import server.quest.actions.*;
import server.quest.requirements.*;
import tools.PacketCreator;
import tools.StringUtil;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Matze
 * @author Ronan - support for medal quests
 */
public class MapleQuest {

    private static volatile Map<Integer, MapleQuest> quests = new HashMap<>();
    private static volatile Map<Integer, Integer> infoNumberQuests = new HashMap<>();
    private static Map<Short, Integer> medals = new HashMap<>();
    
    private static final Set<Short> exploitableQuests = new HashSet<>();
    static {
        exploitableQuests.add((short) 2338);    // there are a lot more exploitable quests, they need to be nit-picked
        exploitableQuests.add((short) 3637);
        exploitableQuests.add((short) 3714);
        exploitableQuests.add((short) 21752);
    }
    
    protected short id;
    protected int timeLimit, timeLimit2;
    protected Map<MapleQuestRequirementType, MapleQuestRequirement> startReqs = new EnumMap<>(MapleQuestRequirementType.class);
    protected Map<MapleQuestRequirementType, MapleQuestRequirement> completeReqs = new EnumMap<>(MapleQuestRequirementType.class);
    protected Map<MapleQuestActionType, MapleQuestAction> startActs = new EnumMap<>(MapleQuestActionType.class);
    protected Map<MapleQuestActionType, MapleQuestAction> completeActs = new EnumMap<>(MapleQuestActionType.class);
    protected List<Integer> relevantMobs = new LinkedList<>();
    private boolean autoStart;
    private boolean autoPreComplete, autoComplete;
    private boolean repeatable = false;
    private String name = "", parent = "";
    private final static DataProvider questData = DataProviderFactory.getDataProvider(WZFiles.QUEST);
    private final static Data questInfo = questData.getData("QuestInfo.img");
    private final static Data questAct = questData.getData("Act.img");
    private final static Data questReq = questData.getData("Check.img");
	
    private MapleQuest(int id) {
        this.id = (short) id;
		
        Data reqData = questReq.getChildByPath(String.valueOf(id));
        if (reqData == null) {//most likely infoEx
            return;
        }
        
        if(questInfo != null) {
            Data reqInfo = questInfo.getChildByPath(String.valueOf(id));
            if(reqInfo != null) {
                name = MapleDataTool.getString("name", reqInfo, "");
                parent = MapleDataTool.getString("parent", reqInfo, "");
                
                timeLimit = MapleDataTool.getInt("timeLimit", reqInfo, 0);
                timeLimit2 = MapleDataTool.getInt("timeLimit2", reqInfo, 0);
                autoStart = MapleDataTool.getInt("autoStart", reqInfo, 0) == 1;
                autoPreComplete = MapleDataTool.getInt("autoPreComplete", reqInfo, 0) == 1;
                autoComplete = MapleDataTool.getInt("autoComplete", reqInfo, 0) == 1;
                
                int medalid = MapleDataTool.getInt("viewMedalItem", reqInfo, 0);
                if(medalid != 0) medals.put(this.id, medalid);
            } else {
                System.out.println("no data " + id);
            }
        }
        
        Data startReqData = reqData.getChildByPath("0");
        if (startReqData != null) {
            for (Data startReq : startReqData.getChildren()) {
                MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(startReq.getName());
                if (type.equals(MapleQuestRequirementType.INTERVAL)) {
                    repeatable = true;
                } else if (type.equals(MapleQuestRequirementType.MOB)) {
                    for (Data mob : startReq.getChildren()) {
                        relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
                    }
                }
		
                MapleQuestRequirement req = this.getRequirement(type, startReq);
                if (req == null) {
                    continue;
                }
		
                startReqs.put(type, req);
            }
        }
        
        Data completeReqData = reqData.getChildByPath("1");
        if (completeReqData != null) {
            for (Data completeReq : completeReqData.getChildren()) {
		MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(completeReq.getName());
                
                MapleQuestRequirement req = this.getRequirement(type, completeReq);
                if (req == null) {
                    continue;
                }
				
                if (type.equals(MapleQuestRequirementType.MOB)) {
                    for (Data mob : completeReq.getChildren()) {
                        relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
                    }
                }
                completeReqs.put(type, req);
            }
        }
        Data actData = questAct.getChildByPath(String.valueOf(id));
        if (actData == null) {
            return;
        }
        final Data startActData = actData.getChildByPath("0");
        if (startActData != null) {
            for (Data startAct : startActData.getChildren()) {
                MapleQuestActionType questActionType = MapleQuestActionType.getByWZName(startAct.getName());
                MapleQuestAction act = this.getAction(questActionType, startAct);
				
                if(act == null)
                    continue;
				
                startActs.put(questActionType, act);
            }
        }
        Data completeActData = actData.getChildByPath("1");
        if (completeActData != null) {
            for (Data completeAct : completeActData.getChildren()) {
                MapleQuestActionType questActionType = MapleQuestActionType.getByWZName(completeAct.getName());
                MapleQuestAction act = this.getAction(questActionType, completeAct);

                if(act == null)
                    continue;
				
                completeActs.put(questActionType, act);
            }
        }
    }
    
    public boolean isAutoComplete() {
        return autoPreComplete || autoComplete;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public static MapleQuest getInstance(int id) {
        MapleQuest ret = quests.get(id);
        if (ret == null) {
            ret = new MapleQuest(id);
            quests.put(id, ret);
        }
        return ret;
    }
    
    public static MapleQuest getInstanceFromInfoNumber(int infoNumber) {
        Integer id = infoNumberQuests.get(infoNumber);
        if (id == null) {
            id = infoNumber;
        }
        
        return getInstance(id);
    }
    
    public boolean isSameDayRepeatable() {
        if(!repeatable) return false;
        
        IntervalRequirement ir = (IntervalRequirement) startReqs.get(MapleQuestRequirementType.INTERVAL);
        return ir.getInterval() < YamlConfig.config.server.QUEST_POINT_REPEATABLE_INTERVAL * 60 * 60 * 1000;
    }
    
    public boolean canStartQuestByStatus(Character chr) {
        QuestStatus mqs = chr.getQuest(this);
        return !(!mqs.getStatus().equals(Status.NOT_STARTED) && !(mqs.getStatus().equals(Status.COMPLETED) && repeatable));
    }
    
    public boolean canQuestByInfoProgress(Character chr) {
        QuestStatus mqs = chr.getQuest(this);
        List<String> ix = mqs.getInfoEx();
        if (!ix.isEmpty()) {
            short questid = mqs.getQuestID();
            short infoNumber = mqs.getInfoNumber();
            if (infoNumber <= 0) {
                infoNumber = questid;  // on default infoNumber mimics questid
            }
            
            int ixSize = ix.size();
            for (int i = 0; i < ixSize; i++) {
                String progress = chr.getAbstractPlayerInteraction().getQuestProgress(infoNumber, i);
                String ixProgress = ix.get(i);
                
                if (!progress.contentEquals(ixProgress)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public boolean canStart(Character chr, int npcid) {
        if (!canStartQuestByStatus(chr)) {
            return false;
        }
        
        for (MapleQuestRequirement r : startReqs.values()) {
            if (!r.check(chr, npcid)) {
                return false;
            }
        }
        
        if (!canQuestByInfoProgress(chr)) {
            return false;
        }
        
        return true;
    }

    public boolean canComplete(Character chr, Integer npcid) {
        QuestStatus mqs = chr.getQuest(this);
        if (!mqs.getStatus().equals(Status.STARTED)) {
            return false;
        }
        
        for (MapleQuestRequirement r : completeReqs.values()) {
            if (!r.check(chr, npcid)) {
                return false;
            }
        }
        
        if (!canQuestByInfoProgress(chr)) {
            return false;
        }
        
        return true;
    }

    public void start(Character chr, int npc) {
        if (autoStart || canStart(chr, npc)) {
            Collection<MapleQuestAction> acts = startActs.values();
            for (MapleQuestAction a : acts) {
                if (!a.check(chr, null)) { // would null be good ?
                    return;
                }
            }
            for (MapleQuestAction a : acts) {
                a.run(chr, null);
            }
            forceStart(chr, npc);
        }
    }

    public void complete(Character chr, int npc) {
        complete(chr, npc, null);
    }

    public void complete(Character chr, int npc, Integer selection) {
        if (autoPreComplete || canComplete(chr, npc)) {
            Collection<MapleQuestAction> acts = completeActs.values();
            for (MapleQuestAction a : acts) {
                if (!a.check(chr, selection)) {
                    return;
                }
            }
            forceComplete(chr, npc);
            for (MapleQuestAction a : acts) {
                a.run(chr, selection);
            }
            if (!this.hasNextQuestAction()) {
                chr.announceUpdateQuest(Character.DelayedQuestUpdate.INFO, chr.getQuest(this));
            }
        }
    }

    public void reset(Character chr) {
        QuestStatus newStatus = new QuestStatus(this, QuestStatus.Status.NOT_STARTED);
        chr.updateQuestStatus(newStatus);
    }

    public boolean forfeit(Character chr) {
        if (!chr.getQuest(this).getStatus().equals(Status.STARTED)) {
            return false;
        }
        if (timeLimit > 0) {
            chr.sendPacket(PacketCreator.removeQuestTimeLimit(id));
        }
        QuestStatus newStatus = new QuestStatus(this, QuestStatus.Status.NOT_STARTED);
        newStatus.setForfeited(chr.getQuest(this).getForfeited() + 1);
        chr.updateQuestStatus(newStatus);
        return true;
    }

    public boolean forceStart(Character chr, int npc) {
        QuestStatus newStatus = new QuestStatus(this, QuestStatus.Status.STARTED, npc);
        
        QuestStatus oldStatus = chr.getQuest(this.getId());
        for (Entry<Integer, String> e : oldStatus.getProgress().entrySet()) {
            newStatus.setProgress(e.getKey(), e.getValue());
        }
        
        if(id / 100 == 35 && YamlConfig.config.server.TOT_MOB_QUEST_REQUIREMENT > 0) {
            int setProg = 999 - Math.min(999, YamlConfig.config.server.TOT_MOB_QUEST_REQUIREMENT);
            
            for(Integer pid : newStatus.getProgress().keySet()) {
                if(pid >= 8200000 && pid <= 8200012) {
                    String pr = StringUtil.getLeftPaddedStr(Integer.toString(setProg), '0', 3);
                    newStatus.setProgress(pid, pr);
                }
            }
        }
        
        newStatus.setForfeited(chr.getQuest(this).getForfeited());
        newStatus.setCompleted(chr.getQuest(this).getCompleted());

        if (timeLimit > 0) {
            newStatus.setExpirationTime(System.currentTimeMillis() + (timeLimit * 1000));
            chr.questTimeLimit(this, timeLimit);
        }
        if (timeLimit2 > 0) {
            newStatus.setExpirationTime(System.currentTimeMillis() + timeLimit2);
            chr.questTimeLimit2(this, newStatus.getExpirationTime());
        }
        
        chr.updateQuestStatus(newStatus);
        
        return true;
    }

    public boolean forceComplete(Character chr, int npc) {
        if (timeLimit > 0) {
            chr.sendPacket(PacketCreator.removeQuestTimeLimit(id));
        }
        
        QuestStatus newStatus = new QuestStatus(this, QuestStatus.Status.COMPLETED, npc);
        newStatus.setForfeited(chr.getQuest(this).getForfeited());
        newStatus.setCompleted(chr.getQuest(this).getCompleted());
        newStatus.setCompletionTime(System.currentTimeMillis());
        chr.updateQuestStatus(newStatus);
        
        chr.sendPacket(PacketCreator.showSpecialEffect(9)); // Quest completion
        chr.getMap().broadcastMessage(chr, PacketCreator.showForeignEffect(chr.getId(), 9), false); //use 9 instead of 12 for both
        return true;
    }

    public short getId() {
        return id;
    }

    public List<Integer> getRelevantMobs() {
        return relevantMobs;
    }

    public int getStartItemAmountNeeded(int itemid) {
        MapleQuestRequirement req = startReqs.get(MapleQuestRequirementType.ITEM);
        if(req == null)
                return Integer.MIN_VALUE;
		
        ItemRequirement ireq = (ItemRequirement) req;
        return ireq.getItemAmountNeeded(itemid, false);
    }
    
    public int getCompleteItemAmountNeeded(int itemid) {
        MapleQuestRequirement req = completeReqs.get(MapleQuestRequirementType.ITEM);
        if(req == null)
                return Integer.MAX_VALUE;
		
        ItemRequirement ireq = (ItemRequirement) req;
        return ireq.getItemAmountNeeded(itemid, true);
    }

    public int getMobAmountNeeded(int mid) {
        MapleQuestRequirement req = completeReqs.get(MapleQuestRequirementType.MOB);
        if(req == null)
            return 0;

        MobRequirement mreq = (MobRequirement) req;

        return mreq.getRequiredMobCount(mid);
    }

    public short getInfoNumber(Status qs) {
        boolean checkEnd = qs.equals(Status.STARTED);
        Map<MapleQuestRequirementType, MapleQuestRequirement> reqs = !checkEnd ? startReqs : completeReqs;

        MapleQuestRequirement req = reqs.get(MapleQuestRequirementType.INFO_NUMBER);
        if (req != null) {
            InfoNumberRequirement inReq = (InfoNumberRequirement) req;
            return inReq.getInfoNumber();
        } else {
            return 0;
        }
    }

    public String getInfoEx(Status qs, int index) {
        boolean checkEnd = qs.equals(Status.STARTED);
        Map<MapleQuestRequirementType, MapleQuestRequirement> reqs = !checkEnd ? startReqs : completeReqs;
        try {
            MapleQuestRequirement req = reqs.get(MapleQuestRequirementType.INFO_EX);
            InfoExRequirement ixReq = (InfoExRequirement) req;
            return ixReq.getInfo().get(index);
        } catch (Exception e) {
            return "";
        }
    }
    
    public List<String> getInfoEx(Status qs) {
        boolean checkEnd = qs.equals(Status.STARTED);
        Map<MapleQuestRequirementType, MapleQuestRequirement> reqs = !checkEnd ? startReqs : completeReqs;
        try {
            MapleQuestRequirement req = reqs.get(MapleQuestRequirementType.INFO_EX);
            InfoExRequirement ixReq = (InfoExRequirement) req;
            return ixReq.getInfo();
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

        public int getTimeLimit() {
                return timeLimit;
        }
	
	public static void clearCache(int quest) {
        quests.remove(quest);
	}
	
	public static void clearCache() {
		quests.clear();
	}
	
	private MapleQuestRequirement getRequirement(MapleQuestRequirementType type, Data data) {
		MapleQuestRequirement ret = null;
		switch(type) {
			case END_DATE:
				ret = new EndDateRequirement(this, data);
				break;
			case JOB:
				ret = new JobRequirement(this, data);
				break;
			case QUEST:
				ret = new QuestRequirement(this, data);
				break;
			case FIELD_ENTER:
				ret = new FieldEnterRequirement(this, data);
				break;
                        case INFO_NUMBER:
                                ret = new InfoNumberRequirement(this, data);
				break;
			case INFO_EX:
				ret = new InfoExRequirement(this, data);
				break;
			case INTERVAL:
				ret = new IntervalRequirement(this, data);
				break;
			case COMPLETED_QUEST:
				ret = new CompletedQuestRequirement(this, data);
				break;
			case ITEM:
				ret = new ItemRequirement(this, data);
				break;
			case MAX_LEVEL:
				ret = new MaxLevelRequirement(this, data);
				break;
                        case MESO:
				ret = new MesoRequirement(this, data);
				break;
			case MIN_LEVEL:
				ret = new MinLevelRequirement(this, data);
				break;
			case MIN_PET_TAMENESS:
				ret = new MinTamenessRequirement(this, data);
				break;
			case MOB:
				ret = new MobRequirement(this, data);
				break;
			case MONSTER_BOOK:
				ret = new MonsterBookCountRequirement(this, data);
				break;
			case NPC:
				ret = new NpcRequirement(this, data);
				break;
			case PET:
				ret = new PetRequirement(this, data);
				break;
                        case BUFF:
				ret = new BuffRequirement(this, data);
				break;
                        case EXCEPT_BUFF:
				ret = new BuffExceptRequirement(this, data);
				break;
			case SCRIPT:
                                ret = new ScriptRequirement(this, data);
                                break;
			case NORMAL_AUTO_START:
			case START:
			case END:
				break;
			default:
				//FilePrinter.printError(FilePrinter.EXCEPTION_CAUGHT, "Unhandled Requirement Type: " + type.toString() + " QuestID: " + this.getId());
				break;
		}
		return ret;
	}
	
	private MapleQuestAction getAction(MapleQuestActionType type, Data data) {
		MapleQuestAction ret = null;
		switch(type) {
			case BUFF:
				ret = new BuffAction(this, data);
				break;
			case EXP:
				ret = new ExpAction(this, data);
				break;
			case FAME:
				ret = new FameAction(this, data);
				break;
			case ITEM:
				ret = new ItemAction(this, data);
				break;
			case MESO:
				ret = new MesoAction(this, data);
				break;
			case NEXTQUEST:
				ret = new NextQuestAction(this, data);
				break;
			case PETSKILL:
				ret = new PetSkillAction(this, data);
				break;
			case QUEST:
				ret = new QuestAction(this, data);
				break;
			case SKILL:
				ret = new SkillAction(this, data);
				break;
                        case PETTAMENESS:
				ret = new PetTamenessAction(this, data);
				break;
                        case PETSPEED:
				ret = new PetSpeedAction(this, data);
				break;
                        case INFO:
                                ret = new InfoAction(this, data);
                                break;
			default:
				//FilePrinter.printError(FilePrinter.EXCEPTION_CAUGHT, "Unhandled Action Type: " + type.toString() + " QuestID: " + this.getId());
				break;
		}
		return ret;
	}
        
        public boolean restoreLostItem(Character chr, int itemid) {
                if (chr.getQuest(this).getStatus().equals(QuestStatus.Status.STARTED)) {
                        ItemAction itemAct = (ItemAction) startActs.get(MapleQuestActionType.ITEM);
                        if (itemAct != null) {
                                return itemAct.restoreLostItem(chr, itemid);
                        }
                }
                
                return false;
        }
	
        public int getMedalRequirement() {
                Integer medalid = medals.get(id);
                return medalid != null ? medalid : -1;
        }
        
        public int getNpcRequirement(boolean checkEnd) {
                Map<MapleQuestRequirementType, MapleQuestRequirement> reqs = !checkEnd ? startReqs : completeReqs;
                MapleQuestRequirement mqr = reqs.get(MapleQuestRequirementType.NPC);
                if (mqr != null) {
                        return ((NpcRequirement) mqr).get();
                } else {
                        return -1;
                }
        }
        
        public boolean hasScriptRequirement(boolean checkEnd) {
                Map<MapleQuestRequirementType, MapleQuestRequirement> reqs = !checkEnd ? startReqs : completeReqs;
                MapleQuestRequirement mqr = reqs.get(MapleQuestRequirementType.SCRIPT);
                
                if (mqr != null) {
                        return ((ScriptRequirement) mqr).get();
                } else {
                        return false;
                }
        }
        
        public boolean hasNextQuestAction() {
                Map<MapleQuestActionType, MapleQuestAction> acts = completeActs;
                MapleQuestAction mqa = acts.get(MapleQuestActionType.NEXTQUEST);
                
                return mqa != null;
        }
        
        public String getName() {
                return name;
        }
        
        public String getParentName() {
                return parent;
        }
        
        public static boolean isExploitableQuest(short questid) {
                return exploitableQuests.contains(questid);
        }
        
        public static List<MapleQuest> getMatchedQuests(String search) {
                List<MapleQuest> ret = new LinkedList<>();
                
                search = search.toLowerCase();
                for (MapleQuest mq : quests.values()) {
                        if (mq.name.toLowerCase().contains(search) || mq.parent.toLowerCase().contains(search)) {
                                ret.add(mq);
                        }
                }
                
                return ret;
        }

    public static void loadAllQuests() {
        final Map<Integer, MapleQuest> loadedQuests = new HashMap<>();
        final Map<Integer, Integer> loadedInfoNumberQuests = new HashMap<>();

        for (Data quest : questInfo.getChildren()) {
            int questID = Integer.parseInt(quest.getName());

            MapleQuest q = new MapleQuest(questID);
            loadedQuests.put(questID, q);

            int infoNumber;

            infoNumber = q.getInfoNumber(Status.STARTED);
            if (infoNumber > 0) {
                loadedInfoNumberQuests.put(infoNumber, questID);
            }

            infoNumber = q.getInfoNumber(Status.COMPLETED);
            if (infoNumber > 0) {
                loadedInfoNumberQuests.put(infoNumber, questID);
            }
        }

        MapleQuest.quests = loadedQuests;
        MapleQuest.infoNumberQuests = loadedInfoNumberQuests;
    }
}
