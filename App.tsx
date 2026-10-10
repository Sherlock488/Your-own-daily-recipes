import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Alert, FlatList, KeyboardAvoidingView, Modal, Platform, Pressable,
  SafeAreaView, ScrollView, StatusBar, StyleSheet, Text, TextInput, View, Image
} from "react-native";
import * as ImagePicker from "expo-image-picker";
import { Camera, ChefHat, ClipboardList, Home, Leaf, Plus, Search, Settings, Trash2, Utensils, X } from "lucide-react-native";
import { RECIPES } from "./src/data/recipes";
import { recommendRecipes } from "./src/lib/recommend";
import { storage } from "./src/lib/storage";
import { Ingredient, PlannedMeal, Preferences, Recipe, StorageLocation } from "./src/types";

type Tab = "Home" | "Kitchen" | "Scan" | "Plan" | "Profile";
const COLORS = { bg: "#FAF8F3", surface: "#FFFFFF", green: "#617A62", dark: "#282B26", muted: "#77796F", border: "#E9E6DE", pale: "#EEF2E9", terracotta: "#D99B7B" };

function Button({ title, onPress, secondary = false }: { title: string; onPress: () => void; secondary?: boolean }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={[styles.button, secondary && styles.buttonSecondary]}><Text style={[styles.buttonText, secondary && styles.buttonSecondaryText]}>{title}</Text></Pressable>;
}
function Label({ children }: { children: React.ReactNode }) {
  return <Text style={styles.label}>{children}</Text>;
}
function Field({ label, value, onChangeText, placeholder, keyboardType = "default", multiline = false }: {
  label: string; value: string; onChangeText: (v: string) => void; placeholder?: string; keyboardType?: "default" | "numeric"; multiline?: boolean
}) {
  return <View style={styles.fieldWrap}><Label>{label}</Label><TextInput value={value} onChangeText={onChangeText} placeholder={placeholder} placeholderTextColor="#9A9A91" keyboardType={keyboardType} multiline={multiline} style={[styles.input, multiline && styles.multiline]} /></View>;
}
function SectionTitle({ title, action, onAction }: { title: string; action?: string; onAction?: () => void }) {
  return <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>{title}</Text>{action && onAction && <Pressable onPress={onAction}><Text style={styles.link}>{action}</Text></Pressable>}</View>;
}
function RecipeCard({ recipe, onPress, onAdd }: { recipe: Recipe; onPress: () => void; onAdd: () => void }) {
  return <View style={styles.recipeCard}>
    <View style={styles.recipeIcon}><Utensils color={COLORS.green} size={22} /></View>
    <View style={{ flex: 1 }}>
      <Text style={styles.recipeName}>{recipe.name}</Text>
      <Text style={styles.bodyMuted}>{recipe.description}</Text>
      <Text style={styles.meta}>{recipe.prepMinutes + recipe.cookMinutes} min · ${recipe.estimatedCost.toFixed(2)} · {recipe.difficulty}</Text>
      <View style={styles.row}><Pressable onPress={onPress}><Text style={styles.link}>View recipe</Text></Pressable><Pressable onPress={onAdd}><Text style={styles.link}>Add to plan +</Text></Pressable></View>
    </View>
  </View>;
}

export default function App() {
  const [tab, setTab] = useState<Tab>("Home");
  const [inventory, setInventory] = useState<Ingredient[]>([]);
  const [plan, setPlan] = useState<PlannedMeal[]>([]);
  const [prefs, setPrefs] = useState<Preferences>({ budget: "10", cookingTime: "30", goal: "Balanced meals", allergies: "", dislikes: "" });
  const [loaded, setLoaded] = useState(false);
  const [ingredientName, setIngredientName] = useState("");
  const [ingredientQuantity, setIngredientQuantity] = useState("1");
  const [ingredientUnit, setIngredientUnit] = useState("pcs");
  const [ingredientLocation, setIngredientLocation] = useState<StorageLocation>("Fridge");
  const [search, setSearch] = useState("");
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null);
  const [scanUri, setScanUri] = useState<string | null>(null);
  const [scanKind, setScanKind] = useState<"ingredients" | "receipt">("ingredients");
  const [scanStatus, setScanStatus] = useState("Choose a photo to begin.");
  const [mealDay, setMealDay] = useState("Today");
  const [addModal, setAddModal] = useState(false);

  useEffect(() => {
    (async () => {
      const [i, p, pr] = await Promise.all([storage.getInventory(), storage.getPlan(), storage.getPreferences()]);
      setInventory(i); setPlan(p); setPrefs(pr); setLoaded(true);
    })();
  }, []);
  useEffect(() => { if (loaded) void storage.setInventory(inventory); }, [inventory, loaded]);
  useEffect(() => { if (loaded) void storage.setPlan(plan); }, [plan, loaded]);
  useEffect(() => { if (loaded) void storage.setPreferences(prefs); }, [prefs, loaded]);

  const recommendations = useMemo(() => recommendRecipes(RECIPES, inventory, {
    maxMinutes: Number(prefs.cookingTime) || undefined,
    budget: Number(prefs.budget) || undefined,
    dislikes: prefs.dislikes,
    allergies: prefs.allergies
  }).slice(0, 4), [inventory, prefs]);

  const addIngredient = () => {
    const name = ingredientName.trim();
    if (!name) { Alert.alert("Ingredient name needed", "Enter an ingredient name first."); return; }
    const qty = ingredientQuantity.trim() ? Number(ingredientQuantity) : null;
    if (qty !== null && (!Number.isFinite(qty) || qty < 0)) { Alert.alert("Check quantity", "Quantity must be a non-negative number."); return; }
    setInventory((old) => [...old, {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      name, quantity: qty, unit: ingredientUnit.trim() || "unit", category: "Other",
      location: ingredientLocation, addedAt: new Date().toISOString()
    }]);
    setIngredientName(""); setIngredientQuantity("1"); setIngredientUnit("pcs"); setAddModal(false);
  };

  const addToPlan = (recipe: Recipe) => {
    setPlan((old) => [...old, { id: `${Date.now()}`, day: mealDay, mealType: "Dinner", recipeId: recipe.id }]);
    Alert.alert("Added to meal plan", `${recipe.name} was added to ${mealDay}'s dinner.`);
  };

  const pickPhoto = async (useCamera: boolean) => {
    try {
      let result: ImagePicker.ImagePickerResult;
      if (useCamera) {
        const permission = await ImagePicker.requestCameraPermissionsAsync();
        if (!permission.granted) { Alert.alert("Camera permission", "Allow camera access in settings, or choose a photo from your library."); return; }
        result = await ImagePicker.launchCameraAsync({ mediaTypes: ["images"], quality: 0.8 });
      } else {
        result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ["images"], quality: 0.8 });
      }
      if (!result.canceled && result.assets[0]) {
        setScanUri(result.assets[0].uri);
        setScanStatus("Photo selected. Real AI recognition requires a configured secure backend.");
      }
    } catch {
      Alert.alert("Unable to open photos", "Please try again or enter ingredients manually.");
    }
  };

  const renderHome = () => <ScrollView contentContainerStyle={styles.page}>
    <View style={styles.eyebrowRow}><Leaf color={COLORS.green} size={16}/><Text style={styles.eyebrow}>YOUR PERSONAL FOOD COMPANION</Text></View>
    <Text style={styles.heroTitle}>What sounds{"\n"}good today?</Text>
    <Text style={styles.heroSub}>A little less deciding. A little more enjoying.</Text>
    <View style={styles.heroCard}>
      <Text style={styles.heroCardKicker}>A FRESH START</Text>
      <Text style={styles.heroCardTitle}>Make something good with what you have.</Text>
      <Text style={styles.bodyMuted}>Scan your ingredients or explore ideas from your kitchen.</Text>
      <View style={styles.rowWrap}><Button title="Scan your food" onPress={() => setTab("Scan")}/><Button title="My kitchen" secondary onPress={() => setTab("Kitchen")}/></View>
    </View>
    <SectionTitle title="Made for you" action="See all" onAction={() => setTab("Kitchen")}/>
    {recommendations.length ? recommendations.slice(0, 3).map(r => <RecipeCard key={r.id} recipe={r} onPress={() => setSelectedRecipe(r)} onAdd={() => addToPlan(r)}/>) :
      <View style={styles.emptyCard}><ChefHat color={COLORS.green} size={26}/><Text style={styles.cardTitle}>Your next meal starts here</Text><Text style={styles.bodyMuted}>Add ingredients or loosen your filters to see more ideas.</Text><Button title="Add ingredients" onPress={() => { setTab("Kitchen"); setAddModal(true); }}/></View>}
    <SectionTitle title="Your kitchen" action="Manage" onAction={() => setTab("Kitchen")}/>
    <View style={styles.summaryRow}><View style={styles.summaryCard}><Text style={styles.summaryNumber}>{inventory.length}</Text><Text style={styles.bodyMuted}>Ingredients</Text></View><View style={styles.summaryCard}><Text style={styles.summaryNumber}>{plan.length}</Text><Text style={styles.bodyMuted}>Planned meals</Text></View></View>
    <Text style={styles.disclaimer}>Nourish offers general meal-planning ideas, not medical advice. Check ingredients and food safety information yourself.</Text>
  </ScrollView>;

  const renderKitchen = () => <ScrollView contentContainerStyle={styles.page}>
    <Text style={styles.pageEyebrow}>YOUR KITCHEN</Text><Text style={styles.pageTitle}>Make the most of what you have.</Text>
    <Text style={styles.bodyMuted}>Keep a simple list of ingredients, then find recipes that use them.</Text>
    <View style={styles.searchBox}><Search color={COLORS.muted} size={18}/><TextInput value={search} onChangeText={setSearch} placeholder="Search ingredients" placeholderTextColor="#9A9A91" style={styles.searchInput}/></View>
    <Button title="+ Add ingredient" onPress={() => setAddModal(true)}/>
    <View style={styles.rowWrap}><Button title="Scan ingredients" secondary onPress={() => {setScanKind("ingredients");setTab("Scan");}}/><Button title="Scan receipt" secondary onPress={() => {setScanKind("receipt");setTab("Scan");}}/></View>
    <SectionTitle title={`Inventory · ${inventory.length}`}/>
    {inventory.filter(i => i.name.toLowerCase().includes(search.toLowerCase())).map(item => <View style={styles.inventoryItem} key={item.id}>
      <View style={styles.ingredientDot}><Leaf color={COLORS.green} size={16}/></View><View style={{flex:1}}><Text style={styles.cardTitle}>{item.name}</Text><Text style={styles.bodyMuted}>{item.quantity ?? "Quantity unknown"} {item.quantity !== null ? item.unit : ""} · {item.location}</Text></View>
      <Pressable accessibilityLabel={`Delete ${item.name}`} onPress={() => Alert.alert("Remove ingredient?", `Remove ${item.name} from your inventory?`, [{text:"Cancel",style:"cancel"},{text:"Remove",style:"destructive",onPress:()=>setInventory(old=>old.filter(x=>x.id!==item.id))}])}><Trash2 color="#A36E5D" size={18}/></Pressable>
    </View>)}
    {!inventory.length && <View style={styles.emptyCard}><Text style={styles.cardTitle}>Your kitchen is ready</Text><Text style={styles.bodyMuted}>Add ingredients manually or choose a photo to start a scan. Photos are not automatically added to inventory.</Text></View>}
  </ScrollView>;

  const renderScan = () => <ScrollView contentContainerStyle={styles.page}>
    <Text style={styles.pageEyebrow}>FOOD SCANNER</Text><Text style={styles.pageTitle}>A picture is a good place to start.</Text>
    <Text style={styles.bodyMuted}>Choose a food photo or grocery receipt. You can review the photo before deciding what to do next.</Text>
    <View style={styles.segmentRow}>
      <Pressable style={[styles.segment, scanKind==="ingredients" && styles.segmentActive]} onPress={()=>{setScanKind("ingredients");setScanUri(null);}}><Text style={[styles.segmentText,scanKind==="ingredients" && styles.segmentTextActive]}>Ingredients</Text></Pressable>
      <Pressable style={[styles.segment, scanKind==="receipt" && styles.segmentActive]} onPress={()=>{setScanKind("receipt");setScanUri(null);}}><Text style={[styles.segmentText,scanKind==="receipt" && styles.segmentTextActive]}>Grocery receipt</Text></Pressable>
    </View>
    <View style={styles.uploadCard}>
      {scanUri ? <Image source={{uri:scanUri}} style={styles.previewImage} resizeMode="contain"/> : <View style={styles.uploadIllustration}><Camera color={COLORS.green} size={34}/><Text style={styles.cardTitle}>{scanKind==="ingredients" ? "What's in your kitchen?" : "What did you bring home?"}</Text><Text style={styles.bodyMuted}>Select a photo to preview it here.</Text></View>}
      <View style={styles.rowWrap}><Button title="Choose photo" onPress={()=>pickPhoto(false)}/><Button title="Take photo" secondary onPress={()=>pickPhoto(true)}/></View>
      {scanUri && <Button title="Remove photo" secondary onPress={()=>{setScanUri(null);setScanStatus("Choose a photo to begin.");}}/>}
    </View>
    <View style={styles.notice}><Text style={styles.noticeTitle}>AI scanning setup</Text><Text style={styles.bodyMuted}>This GitHub starter includes photo selection and a review-first flow. Real recognition is not active until a secure backend and vision AI provider are configured. No fake scan results will be shown.</Text></View>
    <Text style={styles.meta}>{scanStatus}</Text>
    <Button title="Enter ingredients manually" onPress={()=>{setTab("Kitchen");setAddModal(true);}}/>
  </ScrollView>;

  const renderPlan = () => <ScrollView contentContainerStyle={styles.page}>
    <Text style={styles.pageEyebrow}>YOUR WEEK</Text><Text style={styles.pageTitle}>A little plan goes a long way.</Text>
    <Text style={styles.bodyMuted}>Add recipes to a day. Planned ingredients are not deducted from your inventory.</Text>
    <View style={styles.rowWrap}>{["Today","Tomorrow","Wednesday","Thursday","Friday","Saturday","Sunday"].map(day=><Pressable key={day} onPress={()=>setMealDay(day)} style={[styles.dayChip,mealDay===day&&styles.dayChipActive]}><Text style={[styles.dayChipText,mealDay===day&&styles.dayChipTextActive]}>{day}</Text></Pressable>)}</View>
    <SectionTitle title={`${mealDay}'s meals`}/>
    {plan.filter(p=>p.day===mealDay).map(p=>{const r=RECIPES.find(x=>x.id===p.recipeId);return r?<View style={styles.inventoryItem} key={p.id}><View style={styles.ingredientDot}><Utensils color={COLORS.green} size={16}/></View><View style={{flex:1}}><Text style={styles.cardTitle}>{r.name}</Text><Text style={styles.bodyMuted}>{p.mealType} · {r.prepMinutes+r.cookMinutes} min</Text></View><Pressable onPress={()=>setPlan(old=>old.filter(x=>x.id!==p.id))}><X color={COLORS.muted} size={18}/></Pressable></View>:null;})}
    {!plan.some(p=>p.day===mealDay)&&<View style={styles.emptyCard}><ClipboardList color={COLORS.green} size={26}/><Text style={styles.cardTitle}>Nothing planned yet</Text><Text style={styles.bodyMuted}>Choose a recipe below to add it to {mealDay.toLowerCase()}.</Text></View>}
    <SectionTitle title="Ideas for your plan"/>
    {recommendations.map(r=><RecipeCard key={r.id} recipe={r} onPress={()=>setSelectedRecipe(r)} onAdd={()=>addToPlan(r)}/>)}
  </ScrollView>;

  const renderProfile = () => <ScrollView contentContainerStyle={styles.page}>
    <Text style={styles.pageEyebrow}>YOUR PREFERENCES</Text><Text style={styles.pageTitle}>Make Nourish yours.</Text>
    <Text style={styles.bodyMuted}>These preferences help shape local recipe suggestions. Keep allergies up to date and verify ingredient labels yourself.</Text>
    <Field label="Weekly food budget (USD)" value={prefs.budget} onChangeText={v=>setPrefs(p=>({...p,budget:v}))} keyboardType="numeric" placeholder="e.g. 50"/>
    <Field label="Maximum cooking time (minutes)" value={prefs.cookingTime} onChangeText={v=>setPrefs(p=>({...p,cookingTime:v}))} keyboardType="numeric" placeholder="e.g. 30"/>
    <Field label="Food goal" value={prefs.goal} onChangeText={v=>setPrefs(p=>({...p,goal:v}))} placeholder="e.g. Balanced meals"/>
    <Field label="Allergies (comma separated)" value={prefs.allergies} onChangeText={v=>setPrefs(p=>({...p,allergies:v}))} placeholder="e.g. peanuts, shrimp"/>
    <Field label="Ingredients you dislike" value={prefs.dislikes} onChangeText={v=>setPrefs(p=>({...p,dislikes:v}))} placeholder="e.g. mushrooms, fish"/>
    <View style={styles.notice}><Text style={styles.noticeTitle}>Your data</Text><Text style={styles.bodyMuted}>This MVP saves preferences and inventory on this device. It does not sync across devices.</Text><Button title="Clear local app data" secondary onPress={()=>Alert.alert("Clear all local data?", "This removes inventory, plan, and preferences from this device.", [{text:"Cancel",style:"cancel"},{text:"Clear data",style:"destructive",onPress:async()=>{await storage.clear();setInventory([]);setPlan([]);setPrefs({budget:"10",cookingTime:"30",goal:"Balanced meals",allergies:"",dislikes:""});}}])}/></View>
  </ScrollView>;

  const renderContent = () => {
    switch(tab) {
      case "Home": return renderHome();
      case "Kitchen": return renderKitchen();
      case "Scan": return renderScan();
      case "Plan": return renderPlan();
      case "Profile": return renderProfile();
    }
  };

  return <SafeAreaView style={styles.safe}>
    <StatusBar barStyle="dark-content" backgroundColor={COLORS.bg}/>
    <View style={styles.topBar}><View style={styles.brandMark}><Leaf color="#FFFFFF" size={18}/></View><View><Text style={styles.brand}>nourish</Text><Text style={styles.brandSub}>FOOD, MADE PERSONAL</Text></View><Pressable style={styles.topIcon} onPress={()=>setTab("Profile")}><Settings color={COLORS.dark} size={20}/></Pressable></View>
    <View style={styles.content}>{renderContent()}</View>
    <View style={styles.tabBar}>
      {([
        ["Home", Home], ["Kitchen", Utensils], ["Scan", Camera], ["Plan", ClipboardList], ["Profile", Settings]
      ] as const).map(([name, Icon])=><Pressable key={name} onPress={()=>setTab(name)} style={styles.tabItem}>
        <Icon size={19} color={tab===name?COLORS.green:COLORS.muted}/><Text style={[styles.tabLabel,tab===name&&styles.tabLabelActive]}>{name}</Text>
      </Pressable>)}
    </View>

    <Modal visible={addModal} transparent animationType="slide" onRequestClose={()=>setAddModal(false)}>
      <KeyboardAvoidingView behavior={Platform.OS==="ios"?"padding":undefined} style={styles.modalBackdrop}>
        <View style={styles.modalCard}>
          <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>Add ingredient</Text><Pressable onPress={()=>setAddModal(false)}><X color={COLORS.dark}/></Pressable></View>
          <Field label="Ingredient name" value={ingredientName} onChangeText={setIngredientName} placeholder="e.g. Tomatoes"/>
          <View style={styles.row}><View style={{flex:1}}><Field label="Quantity" value={ingredientQuantity} onChangeText={setIngredientQuantity} keyboardType="numeric" placeholder="e.g. 3"/></View><View style={{flex:1}}><Field label="Unit" value={ingredientUnit} onChangeText={setIngredientUnit} placeholder="pcs, g, cups"/></View></View>
          <Label>Storage</Label><View style={styles.rowWrap}>{(["Pantry","Fridge","Freezer"] as StorageLocation[]).map(loc=><Pressable key={loc} onPress={()=>setIngredientLocation(loc)} style={[styles.dayChip,ingredientLocation===loc&&styles.dayChipActive]}><Text style={[styles.dayChipText,ingredientLocation===loc&&styles.dayChipTextActive]}>{loc}</Text></Pressable>)}</View>
          <Button title="Add to kitchen" onPress={addIngredient}/><Button title="Cancel" secondary onPress={()=>setAddModal(false)}/>
        </View>
      </KeyboardAvoidingView>
    </Modal>

    <Modal visible={!!selectedRecipe} transparent animationType="slide" onRequestClose={()=>setSelectedRecipe(null)}>
      <View style={styles.modalBackdrop}><View style={[styles.modalCard,{maxHeight:"88%"}]}>
        <ScrollView>
          <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>Recipe</Text><Pressable onPress={()=>setSelectedRecipe(null)}><X color={COLORS.dark}/></Pressable></View>
          {selectedRecipe&&<><Text style={styles.pageTitle}>{selectedRecipe.name}</Text><Text style={styles.bodyMuted}>{selectedRecipe.description}</Text><Text style={styles.meta}>{selectedRecipe.prepMinutes+selectedRecipe.cookMinutes} min · ${selectedRecipe.estimatedCost.toFixed(2)} · {selectedRecipe.difficulty}</Text><SectionTitle title="Ingredients"/>{selectedRecipe.ingredients.map((i,index)=><Text key={index} style={styles.listLine}>•  {i.quantity} {i.unit} {i.name}</Text>)}<SectionTitle title="Instructions"/>{selectedRecipe.instructions.map((step,index)=><Text key={index} style={styles.listLine}>{index+1}. {step}</Text>)}<Text style={styles.disclaimer}>Estimated cost is illustrative. Check package labels and cook foods safely.</Text><Button title={`Add to ${mealDay}'s plan`} onPress={()=>{addToPlan(selectedRecipe);setSelectedRecipe(null);}}/></>}
        </ScrollView>
      </View></View>
    </Modal>
  </SafeAreaView>;
}

const styles = StyleSheet.create({
  safe:{flex:1,backgroundColor:COLORS.bg},
  topBar:{paddingHorizontal:20,paddingVertical:12,flexDirection:"row",alignItems:"center",gap:10,borderBottomWidth:1,borderBottomColor:COLORS.border},
  brandMark:{width:36,height:36,borderRadius:12,backgroundColor:COLORS.green,alignItems:"center",justifyContent:"center"},
  brand:{fontSize:20,fontWeight:"800",letterSpacing:-0.7,color:COLORS.dark},
  brandSub:{fontSize:9,letterSpacing:1.4,color:COLORS.muted,marginTop:1},
  topIcon:{marginLeft:"auto",padding:8},
  content:{flex:1},
  page:{padding:20,paddingBottom:34,gap:14},
  eyebrowRow:{flexDirection:"row",alignItems:"center",gap:7,marginTop:5},
  eyebrow:{fontSize:10,fontWeight:"800",letterSpacing:1.4,color:COLORS.green},
  heroTitle:{fontSize:38,lineHeight:43,fontWeight:"800",letterSpacing:-1.5,color:COLORS.dark,marginTop:6},
  heroSub:{fontSize:15,color:COLORS.muted,lineHeight:22},
  heroCard:{backgroundColor:"#EEF2E9",borderRadius:24,padding:20,gap:10,marginTop:4},
  heroCardKicker:{fontSize:10,fontWeight:"800",letterSpacing:1.3,color:COLORS.green},
  heroCardTitle:{fontSize:23,lineHeight:28,fontWeight:"800",color:COLORS.dark,letterSpacing:-0.6},
  pageEyebrow:{fontSize:10,fontWeight:"800",letterSpacing:1.4,color:COLORS.green,marginTop:5},
  pageTitle:{fontSize:29,lineHeight:35,fontWeight:"800",letterSpacing:-0.9,color:COLORS.dark},
  bodyMuted:{fontSize:13,lineHeight:19,color:COLORS.muted},
  sectionHeader:{flexDirection:"row",alignItems:"center",justifyContent:"space-between",marginTop:8,marginBottom:2},
  sectionTitle:{fontSize:19,fontWeight:"800",letterSpacing:-0.4,color:COLORS.dark},
  link:{fontSize:13,fontWeight:"700",color:COLORS.green},
  button:{backgroundColor:COLORS.green,borderRadius:14,paddingVertical:13,paddingHorizontal:16,alignItems:"center",justifyContent:"center",minHeight:46},
  buttonText:{color:"#FFFFFF",fontSize:14,fontWeight:"700"},
  buttonSecondary:{backgroundColor:COLORS.surface,borderWidth:1,borderColor:COLORS.border},
  buttonSecondaryText:{color:COLORS.dark},
  row:{flexDirection:"row",alignItems:"center",gap:12},
  rowWrap:{flexDirection:"row",alignItems:"center",gap:8,flexWrap:"wrap"},
  recipeCard:{backgroundColor:COLORS.surface,borderRadius:18,borderWidth:1,borderColor:COLORS.border,padding:14,flexDirection:"row",gap:12,marginBottom:2},
  recipeIcon:{width:46,height:46,borderRadius:14,backgroundColor:COLORS.pale,alignItems:"center",justifyContent:"center"},
  recipeName:{fontSize:16,fontWeight:"800",color:COLORS.dark,marginBottom:4},
  meta:{fontSize:11,fontWeight:"600",color:COLORS.muted,marginTop:7,marginBottom:8},
  emptyCard:{backgroundColor:COLORS.surface,borderRadius:18,borderWidth:1,borderColor:COLORS.border,padding:20,alignItems:"flex-start",gap:10},
  cardTitle:{fontSize:15,fontWeight:"800",color:COLORS.dark},
  summaryRow:{flexDirection:"row",gap:12},
  summaryCard:{flex:1,backgroundColor:COLORS.surface,borderRadius:18,borderWidth:1,borderColor:COLORS.border,padding:18,gap:5},
  summaryNumber:{fontSize:29,fontWeight:"800",color:COLORS.green},
  disclaimer:{fontSize:10,lineHeight:15,color:COLORS.muted,marginTop:8},
  searchBox:{flexDirection:"row",alignItems:"center",gap:9,backgroundColor:COLORS.surface,borderRadius:14,borderWidth:1,borderColor:COLORS.border,paddingHorizontal:12},
  searchInput:{flex:1,minHeight:44,fontSize:14,color:COLORS.dark},
  inventoryItem:{backgroundColor:COLORS.surface,borderRadius:16,borderWidth:1,borderColor:COLORS.border,padding:13,flexDirection:"row",alignItems:"center",gap:12},
  ingredientDot:{width:38,height:38,borderRadius:12,backgroundColor:COLORS.pale,alignItems:"center",justifyContent:"center"},
  fieldWrap:{gap:7,marginVertical:6,flex:1},
  label:{fontSize:12,fontWeight:"700",color:COLORS.dark},
  input:{borderWidth:1,borderColor:COLORS.border,backgroundColor:COLORS.surface,borderRadius:12,paddingHorizontal:12,paddingVertical:11,fontSize:15,color:COLORS.dark,minHeight:46},
  multiline:{minHeight:80,textAlignVertical:"top"},
  segmentRow:{flexDirection:"row",backgroundColor:"#EEECE5",borderRadius:14,padding:4},
  segment:{flex:1,paddingVertical:11,alignItems:"center",borderRadius:11},
  segmentActive:{backgroundColor:COLORS.surface},
  segmentText:{fontSize:13,fontWeight:"700",color:COLORS.muted},
  segmentTextActive:{color:COLORS.dark},
  uploadCard:{backgroundColor:COLORS.surface,borderWidth:1,borderColor:COLORS.border,borderRadius:22,padding:16,gap:12},
  uploadIllustration:{minHeight:190,alignItems:"center",justifyContent:"center",gap:10,backgroundColor:"#F5F5EF",borderRadius:16,padding:20},
  previewImage:{width:"100%",height:260,borderRadius:14,backgroundColor:"#F5F5EF"},
  notice:{backgroundColor:"#F1EEE6",borderRadius:16,padding:15,gap:8},
  noticeTitle:{fontSize:14,fontWeight:"800",color:COLORS.dark},
  dayChip:{paddingHorizontal:13,paddingVertical:10,borderRadius:13,backgroundColor:COLORS.surface,borderWidth:1,borderColor:COLORS.border},
  dayChipActive:{backgroundColor:COLORS.green,borderColor:COLORS.green},
  dayChipText:{fontSize:12,fontWeight:"700",color:COLORS.muted},
  dayChipTextActive:{color:"#FFFFFF"},
  tabBar:{flexDirection:"row",justifyContent:"space-around",alignItems:"center",paddingTop:9,paddingBottom:Platform.OS==="ios"?8:10,backgroundColor:COLORS.surface,borderTopWidth:1,borderTopColor:COLORS.border},
  tabItem:{alignItems:"center",justifyContent:"center",gap:4,minWidth:48,paddingHorizontal:4},
  tabLabel:{fontSize:10,fontWeight:"600",color:COLORS.muted},
  tabLabelActive:{color:COLORS.green,fontWeight:"800"},
  modalBackdrop:{flex:1,justifyContent:"flex-end",backgroundColor:"rgba(25,30,24,0.35)"},
  modalCard:{backgroundColor:COLORS.bg,borderTopLeftRadius:26,borderTopRightRadius:26,padding:20,gap:12},
  listLine:{fontSize:14,lineHeight:22,color:COLORS.dark,marginVertical:3}
});
