<?php

namespace App\Modules\GmTool\Model;

class Quest
{
    public int $questId;

    public function test()
    {
        $a = new Quest(1);
    }

    public function __construct(int $questId)
    {
    }

    /**
     * @param int $episodeId
     * @return Quest[]
     */
    public static function findByEpisodeId(int $episodeId): array
    {
        return [new Quest(1), new Quest(2), new Quest(3)];
    }
}