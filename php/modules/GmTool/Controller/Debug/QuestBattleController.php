<?php

namespace App\Modules\GmTool\Controller\Debug;

use App\Modules\GmTool\Foundation\Controller;
use App\Modules\GmTool\Model\Quest;

class QuestBattleController extends Controller
{
    public function confirmStoreAction()
    {
        $this->setVar('quest', new Quest(1));
        $this->setVar('questIds', [1, 2, 3]);
        $this->setVar('quests', Quest::findByEpisodeId(1));
    }
}