(ns nd.htmlconversions
  (:require [hiccup.core :as h] [clojure.java.jdbc :as jdbc])
)

(def quote (clojure.java.jdbc/quoted \'))

(defn dictionary-entry-to-html [de]
  [:span.gloss
    [:i
      (h/h (de :head))
      [:span.parts (h/h (de :tail))]]
    [:span.english (h/h (de :definition))]
    #_[:span.german  (h/h (de :definition_german))]
    #_[:span.french  (h/h (de :definition_french))]
    #_[:span.italian (h/h (de :definition_italian))]
    #_[:span.spanish (h/h (de :definition_spanish))]])

(defn dictionary-entry-v-value [de]
  (let [frequency (+ 1 (quot (de :global_popularity) 2000))
        quantized-frequency (if (> frequency 9) 10 frequency)]
    (str "v" quantized-frequency)))

(defn passageid-wordguts [db passage-id]
  (clojure.string/join
    " "
    (map :lemmatizable_word
    	 (jdbc/query db ["select lemmatizable_word from text_words t join subpassages s on t.subpassage_id = s.id where s.passage_id = ? order by s.position, t.position"
	 	     	 (+ passage-id 0)]))))


(defn split-before [f lst] (filter (partial not= [nil]) (partition-by nil? (mapcat (fn [e] (if (f e) '(nil e) '(e))) lst))))

(defn text-word-notes-link [twidx tws maybe-note]
  (let [tw (tws twidx)
        index-of-five-before (max 0 (- twidx 5))
	index-of-five-after (min (- (count tws) 1) (+ twidx 5))
	eleven-words (map :lemmatizable_word (subvec tws index-of-five-before (+ 1 index-of-five-after)))]
    (str "/text-word-note/"
    	 (tw :id)
	 "-"
	 (if (nil? maybe-note) "" (str (maybe-note :id) "-"))
	 (clojure.string/join "-" eleven-words))))

(defn ellipse-after-twenty-words [s] (clojure.string/replace s #"(?s)\A(\W*(?:\w+\W+){20})(\w.*)" "$1..."))

(defn unescape-apos [s] (clojure.string/replace s #"&apos;" "'"))  
(def nounescape-apos identity)

(defn ruby-kludge-string-length [s] (count (.getBytes s)))

(defn split-text-words-into-lines [text-words]
  (if (< (ruby-kludge-string-length (clojure.string/join (map :display_word text-words))) 80)
    [text-words]
    (let [is-poetry (every? (fn [line] (> 80 (ruby-kludge-string-length line))) (clojure.string/split-lines (clojure.string/join (map :display_word text-words))))]
      ((reduce
	(fn [[going-size lines] word]
	  (let [display-word (word :display_word)
		new-going-size (+ going-size (ruby-kludge-string-length display-word))]
	    (if (or (> new-going-size 80) (java.util.regex.Pattern/matches "^\\s*\n.*$" display-word))
		[0 (conj lines [word])]
		(let [new-lines (conj (subvec lines 0 (- (count lines) 1)) (conj (last lines) word))]
		  (if (and (not is-poetry) (> new-going-size 30) (java.util.regex.Pattern/matches "^.*[\\n.?,;:()]\\W*$" display-word))
		      [0 (conj new-lines [])]
		      [new-going-size new-lines])))))
	[0 [[]] ]
	text-words) 1))))

(defn tw-dom-id [twid] (str "tw" twid))
(defn tw-dom-lemma-class
  [text-word dictionary-entry]
  (let [self-dom-id (tw-dom-id (text-word :id))]
    (if (and dictionary-entry (= (dictionary-entry :id) (text-word :dictionary_entry_id)))
        (str "lemma " self-dom-id " " (dictionary-entry-v-value dictionary-entry))
        (str "lemma " self-dom-id))))
(defn tw-dom-lemma-id [twid deid] (str "tw-de-" twid "-" deid))

(defn text-word-line-to-html [text-word-line text-words hlemm hdict hnote htwidx]
  [:div.subpassage-line
    [:div.subpassage-line-text [:table {:cellspacing 0} [:tr [:td [:div.nobr
      (map
        (fn [tw]
          (list
            (if (empty? (tw :lemmatizable_word))
              (h/h (tw :display_word))
              (let [dom-id (tw-dom-id (tw :id))]
                [:span.word {:id dom-id
                             :name dom-id
                             :title "Click to see all possible derived definitions" ;; TODO LOCALIZE
                             :onclick (str "javascript:wordvocab('" dom-id "')")} (h/h (clojure.string/trim (tw :display_word)))]))
            " "))
        text-word-line)]]]]]
    ((reduce
       (fn [[preceding-string html] tw]
         #_(println "tw hnote id:" tw hnote (tw :id) (hnote (tw :id)))
         (let [des (sort-by (fn [de] [(Math/abs (- (tw :dictionary_entry_id) (de :id))) (de :id)]) (map hdict (hlemm (tw :lemmatizable_word))))
               des-html (map (fn [de] [:div {:id (tw-dom-lemma-id (tw :id) (de :id)) :class (tw-dom-lemma-class tw de)}
                                        [:table.lh0 {:cellspacing 0} [:tr
                                          (if (not= "" preceding-string) [:td [:span.i (h/h preceding-string)] "&nbsp;"] "")
                                          [:td (dictionary-entry-to-html de)]]]]) des)
               blank-css-class (tw-dom-lemma-class tw nil)
               no-des-html (list [:div {:class blank-css-class} [:span.i (h/h preceding-string)] [:span.gloss "(Currently undefined; we'll fix this soon.)"]]) ;; TODO LOCALIZE
               maybe-des-html (if (empty? des) no-des-html des-html)
               all-public-notes (filter (fn [n] (and (> (n :score) 0) (zero? (n :privacy)))) (hnote (tw :id)))
               note-count (count all-public-notes)
               top-three-direct-public-notes (take 3 (reverse (sort-by :score (filter (fn [n] (zero? (n :parent_id)) ) all-public-notes))))
               notes-html [:table {:cellspacing 0 :class (str (if (empty? top-three-direct-public-notes) "interlinear-note-add" "interlinear-note") " lh0 " blank-css-class)} [:tr
		            (if (empty? preceding-string) "" [:td [:span.i (h/h preceding-string)] "&nbsp;"])
			    [:td
			      (map (fn [n]
				     [:div.interlinear-note-text "&raquo;" [:span.snippet (h/h (ellipse-after-twenty-words (n :note)))]])
				   top-three-direct-public-notes)
			      [(if (empty? top-three-direct-public-notes) :span.interlinear-note-add-text :div.interlinear-note-add-text) ;; div -> span for ruby legacy checks
				[:a {:href (text-word-notes-link (htwidx (tw :id)) text-words nil)}
				  (if (> note-count 3)
				    (str "Read all " note-count " notes about " (tw :lemmatizable_word)) ;; TODO LOCALIZE
				    (if (empty? top-three-direct-public-notes) "Add note" "Read all"))]]]]]] ;; TODO LOCALIZE
	   [(str preceding-string (tw :display_word))
	    (concat html maybe-des-html (list notes-html))]))
	 ["" '()] text-word-line) 1)])

(defn subpassage-to-html [text-words hlemmatizations hdictionary-entries htext-word-notes htext-word-id-positions]
  (let [text-word-lines (split-text-words-into-lines text-words)]
    (interpose "\n" (pmap (fn [line] (text-word-line-to-html line text-words hlemmatizations hdictionary-entries htext-word-notes htext-word-id-positions)) text-word-lines))))

(defn subpassageid-to-components [db i]
  (let [text-words (vec (jdbc/query db ["select * from text_words where subpassage_id = ? order by position" i]))
        user_id (:user_id (first (jdbc/query db ["select user_id from books b join passages p join subpassages sp where b.id = p.book_id and p.id = sp.passage_id and sp.id = ?" i])))
        lemmatizations (jdbc/query db [(str "select lemmatizable_word, dictionary_entry_id from lemmatizations where lemmatizable_word in ("
		       		              (clojure.string/join "," (set (map quote (map :lemmatizable_word text-words))))
					    ") and user_id in (0,?)") user_id])
        hlemmatizations (reduce #(merge-with concat %1 %2) {} (map (fn [lem] {(lem :lemmatizable_word) (list (lem :dictionary_entry_id)) }) lemmatizations))
	deids (set (apply concat (vals hlemmatizations)))
	dictionary-entries (jdbc/query db [(str "select id, head, tail, definition, global_popularity, "
			   	       	  	"coalesce(definition_german, machine_definition_german) as definition_german, "
						"coalesce(definition_french, machine_definition_french) as definition_french, "
						"coalesce(definition_italian, machine_definition_italian) as definition_italian, "
						"coalesce(definition_spanish, machine_definition_spanish) as definition_spanish "
						" from dictionary_entries where id in ("
			   	       	  	  (clojure.string/join "," deids)
			   	       	  	")")])
	hdictionary-entries (reduce merge {} (map (fn [de] {(de :id) de}) dictionary-entries))
	htext-word-notes (group-by :text_word_id
			  (jdbc/query db [(str "select parent_id, privacy, note, text_word_id, sum(case up when 't' then 1 else -1 end) as score"
				       	       " from text_word_notes n join text_word_note_votes v on v.text_word_note_id = n.id where n.text_word_id in ("
					         (clojure.string/join "," (map :id text-words))
					       ") group by n.id")]))
        htext-word-id-positions (reduce merge {} (map-indexed (fn [idx tw] {(tw :id) idx}) text-words))
        ]
    [text-words hlemmatizations hdictionary-entries htext-word-notes htext-word-id-positions]
  )
)

(def subpassage-spacer (map (fn [i] (let [vclass (str "v" i)] [:div.lemma {:class vclass} "&nbsp;"])) [1 1 1 4 9 10]))

(defn subpassageid-to-html [db i]
  (let [[text-words hlemmatizations hdictionary-entries htext-word-notes htext-word-id-positions] (subpassageid-to-components db i)]
    (unescape-apos (h/html (subpassage-to-html text-words hlemmatizations hdictionary-entries htext-word-notes htext-word-id-positions)))
))

#_(defn passage-to-html [db passage personal?]
  (let [personal-query "select subpassage_id from personal_subpassages where personal_passage_id = ?"
        impersonal-query "select id from subpassages where passage_id = ? order by position"
        subpassageids (map :id (jdbc/query db [(if personal? personal-query impersonal-query) (passage :id)]))]
     (h/html [:div.passage-guts
     	       [:div.passage-title (h/h (passage :title))]
	       (pmap (fn [subpassageid] (h/html
                                         [:div.subpassage-guts
					   (subpassageid-to-html subpassageid)
					   subpassage-spacer])) subpassageids)])))
